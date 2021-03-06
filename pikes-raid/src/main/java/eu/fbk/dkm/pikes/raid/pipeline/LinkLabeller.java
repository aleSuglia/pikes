package eu.fbk.dkm.pikes.raid.pipeline;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ixa.kaflib.Dep;
import ixa.kaflib.Dep.Path;
import ixa.kaflib.ExternalRef;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Predicate;
import ixa.kaflib.Predicate.Role;
import ixa.kaflib.Term;

import eu.fbk.dkm.pikes.resources.NAFUtils;
import eu.fbk.dkm.pikes.resources.WordNet;
import eu.fbk.utils.core.Graph;
import eu.fbk.utils.svm.Util;
import eu.fbk.utils.eval.ConfusionMatrix;
import eu.fbk.utils.eval.PrecisionRecall;
import eu.fbk.utils.svm.Classifier;
import eu.fbk.utils.svm.FeatureStats;
import eu.fbk.utils.svm.LabelledVector;
import eu.fbk.utils.svm.Vector;

public class LinkLabeller {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkLabeller.class);

    private static final Map<KAFDocument, Map<Integer, Graph<Term, String>>> GRAPH_CACHE = new MapMaker()
            .weakKeys().makeMap();

    private static final int SELECTED = 1;

    private static final int UNSELECTED = 0;

    private final Classifier classifier;

    @Nullable
    private final Set<String> posPrefixes;

    private LinkLabeller(final Classifier classifier, @Nullable final Iterable<String> posPrefixes) {
        this.classifier = classifier;
        this.posPrefixes = posPrefixes == null ? null : ImmutableSet.copyOf(posPrefixes);
    }

    public static LinkLabeller readFrom(final java.nio.file.Path path) throws IOException {
        final java.nio.file.Path p = Util.openVFS(path, false);
        try {
            final Classifier classifier = Classifier.readFrom(p.resolve("model"));
            final Properties properties = new Properties();
            try (Reader in = Files.newBufferedReader(p.resolve("properties"))) {
                properties.load(in);
            }
            final String pos = properties.getProperty("pos");
            final Set<String> posPrefixes = pos == null ? null : ImmutableSet.copyOf(pos
                    .split(","));
            return new LinkLabeller(classifier, posPrefixes);
        } finally {
            Util.closeVFS(p);
        }
    }

    public Map<Term, Float> label(final KAFDocument document, final Term root) {

        // Identify candidate terms. For each of them, compute features and apply classifier to
        // determine whether candidate term should be selected and with what probability estimate
        // Return a map mapping selected terms to their probability estimates
        final Map<Term, Float> map = Maps.newHashMap();
        for (final Term term : candidates(document, root, this.posPrefixes)) {
            final Vector vector = features(document, root, term);
            final LabelledVector outVector = this.classifier.predict(true, vector);
            final float p = outVector.getProbability(SELECTED);
            if (outVector.getLabel() == SELECTED) {
                map.put(term, p >= 0.5f ? p : 0.5f);
            } else {
                map.put(term, p < 0.5f ? p : 0.5f);
            }
        }
        return map;
    }

    public void writeTo(final java.nio.file.Path path) throws IOException {
        final java.nio.file.Path p = Util.openVFS(path, false);
        try {
            this.classifier.writeTo(p.resolve("model"));
            final Properties properties = new Properties();
            if (this.posPrefixes != null) {
                properties.setProperty("pos", String.join(",", this.posPrefixes));
            }
            try (Writer out = Files.newBufferedWriter(p.resolve("properties"))) {
                properties.store(out, null);
            }
        } finally {
            Util.closeVFS(p);
        }
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof LinkLabeller)) {
            return false;
        }
        final LinkLabeller other = (LinkLabeller) object;
        return this.classifier.equals(other.classifier);
    }

    @Override
    public int hashCode() {
        return this.classifier.hashCode();
    }

    @Override
    public String toString() {
        return "LinkLabeller (" + this.classifier.toString() + ")";
    }

    private static List<Term> candidates(final KAFDocument document, final Term root,
            @Nullable final Set<String> posPrefixes) {

        // We build a larger set of candidate terms, always excluding modifiers (AMOD/NMOD)
        final Set<Term> nonModifierTerms = Sets.newHashSet();

        // Extract all the terms dominated by root that are not coordinated (COORD/CONJ) with
        // root, excluding modifiers (AMOD/NMOD) at any level
        for (final Dep dep : document.getDepsFromTerm(root)) {
            if (!"COORD".equals(dep.getRfunc()) && !"CONJ".equals(dep.getRfunc())) {
                candidatesHelper(document, dep.getTo(), nonModifierTerms);
            }
        }

        // Add all the ancestors of root that are not coordinated (COORD/CONJ) with root. Then,
        // add all the descendents of those ancestors excluding modifiers (AMOD/NMOD) at any level
        for (Dep dep = document.getDepToTerm(root); dep != null; dep = document.getDepToTerm(dep
                .getFrom())) {
            if ("COORD".equals(dep.getRfunc()) || "CONJ".equals(dep.getRfunc())) {
                continue; // exclude terms coordinated with root
            }
            nonModifierTerms.add(dep.getFrom());
            final List<Term> queue = Lists.newArrayList(dep.getFrom());
            while (!queue.isEmpty()) {
                final Term t = queue.remove(0);
                for (final Dep dep2 : document.getDepsFromTerm(t)) {
                    if (dep2.getTo().equals(dep.getTo())) {
                        continue;
                    } else if (!"COORD".equals(dep2.getRfunc()) && !"CONJ".equals(dep2.getRfunc())) {
                        candidatesHelper(document, dep2.getTo(), nonModifierTerms);
                    }
                }
            }
        }

        // Starting from the computed set, we build the final set of candidate terms by
        // considering only terms that matches certain POS tags (extended to consider
        // demonstrative pronouns) and are made only of letters; in case of verbs, we keep only
        // the SRL predicate head
        final List<Term> candidates = Lists.newArrayList();
        final java.util.function.Predicate<Term> matcher = posPrefixes == null ? null //
                : NAFUtils.matchExtendedPos(document, posPrefixes.toArray(new String[0]));
        for (final Term term : document.getTermsBySent(root.getSent())) {
            if (!term.equals(root)
                    && nonModifierTerms.contains(term)
                    && (matcher == null || matcher.test(term))
                    && (!"V".equals(term.getPos()) || term.equals(NAFUtils.syntacticToSRLHead(
                            document, term)))) {
                final String s = term.getStr();
                for (int i = 0; i < s.length(); ++i) {
                    if (Character.isLetter(s.charAt(i))) {
                        candidates.add(term);
                        break;
                    }
                }
            }
        }
        return candidates;
    }

    private static void candidatesHelper(final KAFDocument document, final Term term,
            final Set<Term> nonModifierTerms) {

        // Recursively add term and all its descendants, stopping when a NMOD/AMOD link is found
        nonModifierTerms.add(term);
        for (final Dep dep : document.getDepsFromTerm(term)) {
            final String func = dep.getRfunc();
            if (!"NMOD".equals(func) && !"AMOD".equals(func)) {
                candidatesHelper(document, dep.getTo(), nonModifierTerms);
            }
        }
    }

    private static Vector features(final KAFDocument document, final Term root, final Term node) {

        // Allocate a builder for constructing the feature vector
        final Vector.Builder builder = Vector.builder();

        // Add document ID (not used for training, just for proper CV splitting)
        builder.set("_cluster." + document.getPublic().uri);

        final String rootSST = getReference(root, NAFUtils.RESOURCE_WN_SST, "none") //
                .replaceFirst("[^.]*\\.", "");
        final String nodeSST = getReference(node, NAFUtils.RESOURCE_WN_SST, "none") //
                .replaceFirst("[^.]*\\.", "");
        final Dep rootDep = document.getDepToTerm(root);
        final Dep nodeDep = document.getDepToTerm(node);
        final Boolean rootActive = NAFUtils.isActiveForm(document, root);

        // Add root features
        builder.set("root.pos." + root.getMorphofeat()); // JM
        builder.set("root.dep." + (rootDep == null ? "none" : rootDep.getRfunc())); // JM
        for (final ExternalRef ref : NAFUtils.getRefs(root, NAFUtils.RESOURCE_WN_SYNSET, null)) {
            // builder.set("root.wn." + ref.getReference());
            for (final String synsetID : WordNet.getHypernyms(ref.getReference(), true)) {
                builder.set("root.wn." + synsetID);
            }
        }
        // builder.set("root.word." + root.getStr().toLowerCase()); // JM
        builder.set("root.lemma." + root.getLemma().toLowerCase()); // JM
        builder.set("root.form."
                + (rootActive == null ? "none" : rootActive ? "active" : "passive"));
        builder.set("root.sst." + rootSST);

        // Experimental, disabled root features: SUMO and YAGO types
        // for (final ExternalRef ref : NAFUtils.getRefs(root, NAFUtils.RESOURCE_SUMO, null)) {
        // builder.set("root.sumo." + ref.getReference());
        // for (final URI uri : Sumo.getSuperClasses(new URIImpl(Sumo.NAMESPACE
        // + ref.getReference()))) {
        // builder.set("root.sumo." + uri.getLocalName());
        // }
        // }
        // for (final ExternalRef ref : NAFUtils.getRefs(root, NAFUtils.RESOURCE_YAGO, null)) {
        // builder.set("root.yago." + ref.getReference());
        // for (final URI uri : YagoTaxonomy.getSuperClasses(new URIImpl(YagoTaxonomy.NAMESPACE
        // + ref.getReference()), true)) {
        // builder.set("root.yago." + uri.getLocalName());
        // }
        // }

        // Add node features
        builder.set("node.pos." + node.getMorphofeat()); // JM
        builder.set("node.dep." + (nodeDep == null ? "none" : nodeDep.getRfunc())); // JM
        for (final ExternalRef ref : NAFUtils.getRefs(node, NAFUtils.RESOURCE_WN_SYNSET, null)) {
            // builder.set("node.wn." + ref.getReference());
            for (final String synsetID : WordNet.getHypernyms(ref.getReference(), true)) {
                builder.set("node.wn." + synsetID);
            }
        }
        // builder.set("node.word." + node.getStr().toLowerCase()); // JM
        builder.set("node.lemma." + node.getLemma().toLowerCase());
        builder.set("node.named", node.getMorphofeat().startsWith("NNP"));
        builder.set("node.bbn.", getReferences(node, NAFUtils.RESOURCE_BBN));
        builder.set("node.sst." + nodeSST);

        // Experimental, disabled node features: SUMO and YAGO types
        // for (final ExternalRef ref : NAFUtils.getRefs(node, NAFUtils.RESOURCE_SUMO, null)) {
        // builder.set("node.sumo." + ref.getReference());
        // for (final URI uri : Sumo.getSuperClasses(new URIImpl(Sumo.NAMESPACE
        // + ref.getReference()))) {
        // builder.set("node.sumo." + uri.getLocalName());
        // }
        // }
        // for (final ExternalRef ref : NAFUtils.getRefs(node, NAFUtils.RESOURCE_YAGO, null)) {
        // builder.set("node.yago." + ref.getReference());
        // for (final URI uri : YagoTaxonomy.getSuperClasses(new URIImpl(YagoTaxonomy.NAMESPACE
        // + ref.getReference()), true)) {
        // builder.set("node.yago." + uri.getLocalName());
        // }
        // }

        // Add context features (their impact is minimal, hence we disabled them)
        // final int index = document.getTerms().indexOf(node);
        // final Term left = index == 0 ? null : document.getTerms().get(index - 1);
        // final Term right = index == document.getTerms().size() - 1 ? null : document.getTerms()
        // .get(index + 1);
        // builder.set("left.pos." + (left == null ? "none" : left.getMorphofeat())); // JM
        // builder.set("left.word." + (left == null ? "none" : left.getStr().toLowerCase())); //
        // JM
        // builder.set("right.pos." + (right == null ? "none" : right.getMorphofeat())); // JM
        // builder.set("right.word." + (right == null ? "none" : right.getStr().toLowerCase()));
        // // JM

        // Add path features
        final Dep.Path path = Dep.Path.create(root, node, document);
        builder.set("path." + (path == null ? "none" : getSimplifiedPathLabel(path)));
        for (int i = 0; i < 10; ++i) {
            if (path.length() <= i) {
                builder.set("path.lenless." + i);
            }
        }

        // Add SRL features
        final Graph<Term, String> graph = getSRLGraph(document, root.getSent());
        for (final Graph.Path<Term, String> srlPath : graph.getPaths(root, node, false, 2)) {
            final StringBuilder b = new StringBuilder("srl.path.");
            for (int i = 0; i < srlPath.length(); ++i) {
                final Term term = srlPath.getVertices().get(i);
                final Graph.Edge<Term, String> edge = srlPath.getEdges().get(i);
                if (i > 0 && term.getMorphofeat().startsWith("VB")
                        && !document.getPredicatesByTerm(term).isEmpty()) {
                    b.append("_").append(term.getLemma().toLowerCase());
                }
                b.append(edge.getSource().equals(term) ? "_F_" : "_B_").append(edge.getLabel());
            }
            builder.set(b.toString());
        }

        // Finalize the construction and return the resulting feature vector
        return builder.build();
    }

    private static String getSimplifiedPathLabel(final Path path) {

        // Filter out coordination edges (COORD|CONJ) and keep track of remaining edge direction
        // (U=up, D=down in the tree)
        final StringBuilder builder = new StringBuilder();
        Term term = path.getTerms().get(0);
        for (int i = 0; i < path.getDeps().size(); ++i) {
            final Dep dep = path.getDeps().get(i);
            final String func = dep.getRfunc().toLowerCase();
            if ("coord".equals(func) || "conj".equals(func)) {
                continue;
            }
            builder.append(func);
            if (term.equals(dep.getFrom())) {
                builder.append('D');
                term = dep.getTo();
            } else {
                builder.append('U');
                term = dep.getFrom();
            }
        }
        return builder.toString();
    }

    @Nullable
    private static String getReference(final Object annotation, final String resource,
            @Nullable final String defaultValue) {
        final List<String> refs = getReferences(annotation, resource);
        if (refs.isEmpty()) {
            return defaultValue;
        } else if (refs.size() == 1) {
            return refs.get(0);
        } else {
            Collections.sort(refs);
            return refs.get(0);
        }
    }

    private static List<String> getReferences(final Object annotation, final String resource) {
        final List<String> result = Lists.newArrayList();
        for (final ExternalRef ref : NAFUtils.getRefs(annotation, resource, null)) {
            result.add(ref.getReference());
        }
        return result;
    }

    private static Graph<Term, String> getSRLGraph(final KAFDocument document, final int sentence) {

        // Lookup an existing graph in the cache
        synchronized (GRAPH_CACHE) {
            final Map<Integer, Graph<Term, String>> map = GRAPH_CACHE.get(document);
            if (map != null) {
                final Graph<Term, String> graph = map.get(sentence);
                if (graph != null) {
                    return graph;
                }
            }
        }

        // Build graph
        final Graph.Builder<Term, String> builder = Graph.builder();
        for (final Predicate predicate : document.getPredicates()) {
            final Term predicateHead = NAFUtils.extractHead(document, predicate.getSpan());
            if (predicateHead != null) {
                for (final Role role : predicate.getRoles()) {
                    for (final Term argHead : NAFUtils.extractHeads(document, null, role
                            .getTerms(), NAFUtils.matchExtendedPos(document, "NN", "PRP", "JJP",
                            "DTP", "WP", "VB"))) {
                        builder.addEdges(predicateHead, argHead, role.getSemRole());
                    }
                }
            }
        }

        // Finalize graph construction
        final Graph<Term, String> graph = builder.build();
        synchronized (GRAPH_CACHE) {
            Map<Integer, Graph<Term, String>> map = GRAPH_CACHE.get(document);
            if (map == null) {
                map = Maps.newHashMap();
                GRAPH_CACHE.put(document, map);
            }
            map.put(sentence, graph);
        }
        return graph;
    }

    public static Trainer train(final String... posPrefixes) {
        final Set<String> posPrefixesSet = posPrefixes == null || posPrefixes.length == 0 ? null
                : ImmutableSet.copyOf(posPrefixes);
        return new Trainer(posPrefixesSet);
    }

    public static final class Trainer {

        private final List<LabelledVector> trainingSet;

        private final PrecisionRecall.Evaluator coverageEvaluator;

        @Nullable
        private final Set<String> posPrefixes;

        private Trainer(@Nullable final Set<String> posPrefixes) {
            this.trainingSet = Lists.newArrayList();
            this.posPrefixes = posPrefixes;
            this.coverageEvaluator = PrecisionRecall.evaluator();
        }

        public void add(final KAFDocument document, final Term root, final Iterable<Term> selected) {

            int numSelected = 0;
            final List<Term> candidates = candidates(document, root, this.posPrefixes);
            for (final Term term : candidates) {
                final int label = Iterables.contains(selected, term) ? SELECTED : UNSELECTED;
                numSelected += label == SELECTED ? 1 : 0;
                final LabelledVector vector = features(document, root, term).label(label);
                this.trainingSet.add(vector);
            }

            final int goldSelected = Iterables.size(selected);
            if (goldSelected > 0) {
                this.coverageEvaluator.add(numSelected, 0, goldSelected - numSelected);

                if (numSelected < goldSelected && LOGGER.isDebugEnabled()) {
                    final List<Term> unselected = Lists.newArrayList();
                    for (final Term term : selected) {
                        if (!candidates.contains(term)) {
                            unselected.add(term);
                        }
                    }
                    LOGGER.debug("Missing candidates: "
                            + unselected
                            + " (root: "
                            + root.toString()
                            + ", sentence: "
                            + KAFDocument.newTermSpan(document.getTermsBySent(root.getSent()))
                                    .getStr() + ")");
                }
            }
        }

        public LinkLabeller end(final int gridSize, final boolean analyze) throws IOException {

            // Emit feature stats if enabled
            if (analyze && LOGGER.isInfoEnabled()) {
                LOGGER.info("Feature analysis (top 30 features):\n{}", FeatureStats.toString(
                        FeatureStats.forVectors(2, this.trainingSet, null).values(), 30));
            }

            // Log the performance penalty caused by the candidate selection algorithm
            LOGGER.info("Maximum achievable performances on training set due to candidate "
                    + "selection criteria: " + this.coverageEvaluator.getResult());

            // Perform training considering a grid of parameters of the size specified (min 1)
            final List<Classifier.Parameters> grid = Lists.newArrayList();
            for (final float weight : new float[] { 0.25f, 0.5f, 1.0f, 2.0f, 4.0f }) {
                // SVM classifiers produce better CV results on training, but overfits on test
                // grid.addAll(Classifier.Parameters.forSVMPolyKernel(2, new float[] { 1f, weight
                // }, 1f, 1f, 0.0f, 3).grid(Math.max(1, gridSize), 10.0f));
                // grid.addAll(Classifier.Parameters.forSVMLinearKernel(2,
                // new float[] { 1f, weight }, 1f).grid(Math.max(1, gridSize), 10.0f));
                grid.addAll(Classifier.Parameters.forLinearLRLossL1Reg(2,
                        new float[] { 1f, weight }, 1f, 1f).grid(Math.max(1, gridSize), 10.0f));
            }
            final Classifier classifier = Classifier.train(grid, this.trainingSet,
                    ConfusionMatrix.labelComparator(PrecisionRecall.Measure.F1, 1, true), 100000);

            // Log parameters of the best classifier
            LOGGER.info("Best classifier parameters: {}", classifier.getParameters());

            // Perform cross-validation and emit some performance statistics, if enabled
            if (analyze && LOGGER.isInfoEnabled()) {
                final List<LabelledVector> trainingPredictions = classifier.predict(false,
                        this.trainingSet);
                final ConfusionMatrix matrix = LabelledVector.evaluate(this.trainingSet,
                        trainingPredictions, 2);
                LOGGER.info("Performances on training set:\n{}", matrix);
                final ConfusionMatrix crossMatrix = Classifier.crossValidate(
                        classifier.getParameters(), this.trainingSet, 5, -1);
                LOGGER.info("5-fold cross-validation performances:\n{}", crossMatrix);
            }

            // Build and return the created link labeller
            return new LinkLabeller(classifier, this.posPrefixes);
        }

    }

}

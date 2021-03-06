Download and run PIKES
===

PIKES only works out-of-the-box on GNU/Linux machines (tested on Debian, Ubuntu and Red Hat). It works also on Mac OS X,
but the UKB module (word sense disambiguation) should be installed separately (see below).

The software needs Java 1.8 and at least 8GB of RAM (better 12G) for the models.

We provide a single [full package](https://knowledgestore.fbk.eu/files/pikes/download/pikes-all.tar.gz), containing all modules, models, and configurations needed to run PIKES straightaway.
The package includes:

* [PIKES Java core library](https://knowledgestore.fbk.eu/files/pikes/download/pikes-tintop-1.0-SNAPSHOT-jar-with-dependencies.jar)
* [Semafor library](https://knowledgestore.fbk.eu/files/pikes/download/Semafor-3.0-alpha-04.jar)<br />
SEMAFOR is a frame-semantic parser for English.
[(More info)](http://www.cs.cmu.edu/~ark/SEMAFOR/)
[(Source code)](https://github.com/Noahs-ARK/semafor)
[(License)](https://github.com/Noahs-ARK/semafor/blob/master/LICENSE)
* [PIKES models](https://knowledgestore.fbk.eu/files/pikes/download/models.tar.gz)<br />
This package contains the models for some linguistic tools included in PIKES:
[Mate-tools](https://code.google.com/archive/p/mate-tools/),
[Semafor](http://www.cs.cmu.edu/~ark/SEMAFOR/),
[PredicateMatrix](http://adimen.si.ehu.es/web/PredicateMatrix),
[Stanford CoreNLP](http://stanfordnlp.github.io/CoreNLP/).
* [WordNet 3.0 package](https://knowledgestore.fbk.eu/files/pikes/download/wordnet.tar.gz)<br />
WordNet is a large lexical database of English developed at Princeton University. Nouns, verbs, adjectives and adverbs are grouped into sets of cognitive synonyms (synsets), each expressing a distinct concept. Synsets are interlinked by means of conceptual-semantic and lexical relations.
[(More info)](https://wordnet.princeton.edu/wordnet/)
[(License)](https://wordnet.princeton.edu/wordnet/license/)
* [UKB](https://knowledgestore.fbk.eu/files/pikes/download/ukb.tar.gz)<br />
UKB is a collection of programs for performing graph-based Word Sense Disambiguation and lexical similarity/relatedness using a pre-existing knowledge base.
[(More info)](http://ixa2.si.ehu.es/ukb/)
[(Source code)](https://github.com/asoroa/ukb)
[(License)](https://github.com/asoroa/ukb/blob/master/src/LICENSE)


Run PIKES on GNU/Linux
---

If you want to run PIKES on GNU/Linux out-of-the box, just execute the following commands on a Bash shell.

```
wget https://knowledgestore.fbk.eu/files/pikes/download/pikes-all.tar.gz # Download the full package
tar xzf pikes-all.tar.gz
cd pikes/
./run.sh
```

After a minute, PIKES should be active on port 8011 (you can change the port modifying the PORT variable in the run.sh script file).
Run

```
java -Xmx8G eu.fbk.dkm.pikes.tintop.server.PipelineServer -h
```

for the complete list of PIKES parameters.
To test it, go to a browser that can reach the machine you run PIKES into, and surf to

```
http://server:8011/text2naf?text=G.%20W.%20Bush%20and%20Bono%20are%20very%20strong%20supporters%20of%20the%20fight%20of%20HIV%20in%20Africa.%20Their%20March%202002%20meeting%20resulted%20in%20a%205%20billion%20dollar%20aid.
```

where `server` is the name of the server (i.e. `localhost`).
If you can see the [NAF](http://www.newsreader-project.eu/files/2013/01/techreport.pdf), it means that PIKES is working well.

If you need the [TRiG](https://www.w3.org/TR/trig/) (instead of NAF), just change `text2naf` to `text2rdf`.

There is also a web interface (such as the web demo available on the [PIKES web site](https://knowledgestore2.fbk.eu/pikes-demo/));
you need [graphviz](http://www.graphviz.org/) to be installed on the server to run it.
With Debian/Ubuntu, just run `apt-get install graphviz` and restart PIKES.

The demo interface (with input textbox for text) is written in php and available under the `src/webdemo/` folder in the project.
To access it, just surf to `http://server:8011/webdemo`.

PIKES can be executed without a configuration file, as there is a default properties file.
The properties values are stored in the `eu.fbk.dkm.pikes.tintop.Defaults` class.
You can override these values by creating a configuration file (e.g., the `config-pikes.prop` file included in the PIKES package) and pass it to the `PipelineServer` (`-c` option, cf. `run.sh`).
If you want to pass Stanford CoreNLP configurations, just prepend `stanford.` to the name of the preference.
For example, to override the list of the annotators you can create a config file with
`stanford.annotators = tokenize, ssplit` and you'll have only tokenizer and sentence splitter.

By default, the text lenght is limited to 1000 characters.
You can override it by adding the `max_text_len` property in the configuration file.

Run PIKES on a Mac
---

To execute PIKES on a Mac OS X machine, you need to recompile UKB, that needs [boost](http://www.boost.org/)
version 1.44 or higher.
If you have [Homebrew](http://brew.sh/) installed, just run `brew install boost`, otherwise you need to download
and compile boost.

Then run:

```
git clone https://github.com/asoroa/ukb
cd ukb/src/
./configure
make
```

Finally, copy `compile_kb`, `convert2.0`, `ukb_ppv` and `ukb_wsd` to the `ukb/` folder in the running directory.

During the `./configure` command, you may need to specify where boost has been installed using the
`--with-boost-include` parameter. If you used Homebrew, you should add
`--with-boost-include=/usr/local/Cellar/boost/1.63.0/include` (replace `1.63.0` with the version you installed).

Recompile PIKES from sources
---

If you want to generate the core library from source, just execute:

```
git clone https://github.com/fbk/fcw
cd fcw/
mvn clean install -DskipTests
cd ..
git clone https://github.com/dkmfbk/pikes
cd pikes/
git checkout develop
mvn clean package -DskipTests -Prelease
```

You'll get the `pikes-tintop-1.0-SNAPSHOT-jar-with-dependencies.jar` package into the `pikes-tintop/target/` folder.
Just copy it to the running folder and restart PIKES.

The FCW package is still under development, it is not available on Maven Central yet,
and therefore one needs to compile and install it from sources. 


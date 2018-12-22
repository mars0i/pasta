# pasta

Agent-based simulation<a href="#WhatsABMnote" id="WhatsABMref"><sup>1</sup></a> with:

* A very simple model of individual learning by prediction error
minimization<a href="#WhatsPEMnote" id="WhatsPEMref"><sup>2</sup></a>
(PEM) in some agents, known as "k-snipes".  (I do mean *model*; what
these agents do, internally, is much simpler than what's usually meant
by PEM.)

* Agents that don't learn, but produce different types of offspring
that are well suited or poorly suited for survival in different
environments.  These agents are known as "r-snipes".

* Agents that engage in a simple form of social learning by copying
  from nearby agents ("s-snipes").

<a href="http://members.logical.net/~marshall">Marshall Abrams</a>

See info about LICENSE below.

## More info

The name of this project comes from the r-snipes' strategy of
producing lots of offspring, many of whom will be maladapted to their
environment and die.  Kind of like testing pasta by throwing it against
the wall to see what sticks.

k-snipes implement a certain kind of evolutionary  <a
href="http://www.oxfordreference.com/view/10.1093/acref/9780199766444.001.0001/acref-9780199766444-e-3642?rskey=KrGgBD&result=1">K
strategy</a>, while r-snipes implement a certain kind of evolutionary <a
href="http://www.oxfordreference.com/view/10.1093/acref/9780199766444.001.0001/acref-9780199766444-e-6006?rskey=XfTY4o&result=1">r
strategy</a>.  (These terms are not always defined in precisely the same
way.)  s-snipes, or "social snipes", it turns out, also implement an r-strategy.

Note that in this model, the K vs r strategies have nothing to do with
K vs r selection in the sense of responding to having or not having
bounded resources or population size.  The simulation produces
qualitatively similar results whether the population is growing or has
reached its maximum size, for example.

My starting points on PEM are Rafal Bogacz, "A tutorial on the
free-energy framework for modeling perception and learning", *Journal
of Mathematical Psychology* 76 Part B, Feb. 2017, pp. 198-211,
http://dx.doi.org/10.1016/j.jmp.2015.11.003, and Harriet Feldman and
Karl Friston, "Attention, Uncertainty, and Free-Energy", *Frontiers in
Human Neuroscience* 4, 2010,
http://dx.doi.org/10.3389/fnhum.2010.00215.  However, the model
implemented here in the k-snipe agents is very simple and very
different.  Andy Clark's <em>Surfing Uncertainty</em> (Oxford 2015)
and Jakob Hohwy's <em>The Predictive Mind</em> (Oxford 2014) contain
non-technical discussions of PEM.

## Overview of simulation

The point is to compare evolutionary strategies and see which is
selected for under specific parameter combinations.  One strategy, a
variant of what's called a "K strategy", here uses a model of learning
that I view as a model of prediction error minimization.  The other
strategy, a variant of what's known as an r strategy, here produces
offspring that are simply predisposed to certain behaviors and don't
learn.  The *general* concepts are that r strategists tend to produce
more offspring, but many of them die without reproduction, while K
strategists are less likely to die, but produce fewer offspring.
Roughly, an r strategy prioritizes quantity over quality, while a K
strategy does the opposite.

Snipes eat mushrooms.<a id="joke1ref" href="#joke1"><sup>3</sup></a>
There are two environments.  In the *east* (usually left)
environment, small mushrooms are nutritious (gray-grown-green) and large mushrooms
are poisonous (greenish yellow).  In the *west* (usually right)
environment, large mushrooms are nutritious and small mushrooms are
poisonous.

Snipes gain energy from eating nutritious mushrooms, and lose energy
from eating poisonous mushrooms.  Snipes that accumulate sufficient
energy give birth, and lose energy as a result of the birth.  At
birth, each newborn snipe is placed at a random location in a randomly
chosen environment.

*k-snipes* (red circles with pointers) initially eat mushrooms randomly,
but learn to eat mushrooms whose size signal (which is normally
distributed) indicates that they are probably nutritious.  The direction
of a k-snipe's pointer--how far up or down its pointing--indicates the
degree of the snipe's preference for large or small mushrooms.  For more
details, see <a
href="https://github.com/mars0i/pasta/blob/master/doc/kSnipePerception.pdf">https://github.com/mars0i/pasta/blob/master/doc/kSnipePerception.pdf</a>.</p>

*r-snipes* (blue triangles) never learn.  They produce offspring that
exhibit developmental differences: Roughly half of any snipe's
offspring (upward-pointing triangles) always prefer large mushrooms;
the others (downward-pointing triangles) always prefer small
mushrooms.  Those suited to the environment in which they live tend to
survive and reproduce, and those unsuited to their environment
generally die before reproduction.

*s-snipes* (purple wing shapes) use a social learning or cultural
transmission strategy known as "success bias".  This is a form of
"model bias" because it's a bias toward learning from "models"
(teachers, influencers) who have certain properties. A newborn s-snipe
examines nearby snipes and copies the current mushroom size preference
of whichever nearby snipe has the most energy.  If there are no nearby
snipes, the s-snipe tries again on the next timestep.  Once it adopts
a preference, an s-snipe's preference never changes.  
The direction in which an s-snipe points--how far up or down it is
tilted---indicates the degree of the snipe's preference for large or
small mushrooms.

(For more on cultural
transmission biases see e.g. Richerson and Boyd's <em>Not by Genes
Alone</em>, University of Chicago Press 2006.)

Snipes' energy levels are reflected in their brightness, with greater
brightness indicating more energy.  Mushroom color indicates
nutritiousness/poisonousness:  Lighter colors indicate
nutritiousness--i.e. these mushrooms are energetically favorable to
snipes---while darker colors indicate poisonousness---energetically
unfavorability.  (The difference in shades between environments has no
functional meaning, but can be thought of as indicating a difference
between mushroom species.)

## Running

Pasta has a number of parameters that can be configured between runs (or
sometimes during runs) using sliders or text boxes on the "Model" tab in
the GUI, or through command line options in the no-GUI version.  See
below.  I haven't written much more documentation at this point, but
would be happy to answer questions.  You can probably figure out a lot
through experimentation, though.

Ways to run pasta:

(1) Download <a
href="http://members.logical.net/~marshall/pasta.jar">pasta.jar</a> to your
computer.  If your computer is configured appropriately, you should be
able to double-click or click on this file to run pasta.  If not, and
you're comfortable with a terminal window (OSX terminal, Windows
cmd.exe, any shell window in Linux), you can run pasta using this
command:

    java -jar pasta.jar

If you're not in the directory where you've put pasta.jar, you might
have to add a path to the filename.  You can use command line options to
run pasta in no-GUI mode.  See below.  


(2) With a full installation (see below) running `src/scripts/gui` or
`src/scripts/pasta` will start the GUI version of pasta.

(3) With a full installation (see below), running `src/scripts/nogui`,
or running `src/scripts/pasta` with no-GUI command line options will start the
command line version (unless `-g` is specified).  You may want to run it
with `-?` as argument first to see the possible command line options.

(4) With a full installation (see below) running `src/scripts/repl` will
start the Clojure REPL.  This is the same as running `lein repl`, but
displays some helpful suggestions.  e.g. it suggests that you can start
the GUI using some variation on the following Clojure commands:

   (use 'pasta.UI) (def cfg (repl-gui))

If you run these, the GUI should start up, and you can run the program
from there. However, you can also use the REPL to examine the state of
the program:

    (def cfg-data$ (.simConfigData cfg))

This defines `cfg-data$` as a Clojure `atom` containing structure
(essentially a Clojure map) with various sorts of parameters and
runtime state.  (I follow a non-standard convention of naming
variables containing atoms with a dollar sign character as suffix.)
For example, all currently living snipes are listed in map called
`:snipes`, keyed by id, in the `:popenv` structure in the structure to
which `cfg-data$` refers. For example, since ids are assigned
sequentially, the largest id is the count of all snipes that have
lived:

    (apply max (keys (:snipes (:popenv @cfg-data$))))

*Warning:* Unfortunately, snipes contain a reference to cfg-data$ itself,
and by default the REPL will try to list the contents of atoms, so if
you allow any snipe to print to the terminal, you'll set off an
infinite loop that will result in a stack overflow.  Sorry about that!


## Full installation

This program is written in the [Clojure](https://clojure.org) language
using [MASON](http://cs.gmu.edu/~eclab/projects/mason).  MASON is a
Java library for agent-based modeling.

To install, once you clone this repo:

You need Leiningen (http://leiningen.org).  Then change to the root
directory of the repo, and run 'lein deps'.  This will install the right
version of Clojure, if necessary, along with a number of Clojure
libraries and such that pasta needs.

You'll also need to download the MASON jar file mason.19.jar (or a later
version, probably), and MASON libraries.tar.gz or libraries.zip file.
Move the MASON jar file into the lib directory under this project's
directory. Unpack the contents of the libraries file into this directory
as well.   You may want to get the MASON-associated 3D libs, too, in
order to get rid of a warning message during startup.  Place these
jars in the same places as the others.


## License

This software is copyright 2016, 2017 by [Marshall
Abrams](http://members.logical.net/~marshall/), and is distributed
under the [Gnu General Public License version
3.0](http://www.gnu.org/copyleft/gpl.html) as specified in the file
LICENSE, except where noted, or where code has been included that was
released under a different license.

##

<a id="WhatsABMnote" href="#WhatsABMref"><sup>1</sup></a> "Agent-based" here refers to the loose class of <a href="https://en.wikipedia.org/wiki/Agent-based_model">simulations</a>
in which outcomes of interest come from interactions between many
semi-independent entities--*agents*--which often model behaviors or
interactions between people, organisms, companies, etc.  "Agent-based"
does *not* refer to various ways of dealing with concurrency, as for
example with Clojure's <a href="https://clojure.org/reference/agents">agent</a> data structure.

<a id="WhatsPEMnote" href="#WhatsPEMref"><sup>2</sup></a> Prediction energy minimization: AKA free-energy minimization, predictive
processing, predictive coding. cf. integral control, mean-field
approximation, variational methods.

<a id="joke1" href="#joke1ref"><sup>3</sup></a>Yes--this pasta has mushrooms in it.  Yum!

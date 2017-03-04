# pasta

Agent-based simulation with:

* A very simple model of individual learning by prediction error
minimization<a href="#WhatsPEMnote" id="WhatsPEMref"><sup>1</sup></a>
(PEM) in some agents, known as "k-snipes".  (I do mean *model*; what
these agents do, internally, is much simpler than what's usually meant
by PEM.)

* Agents that don't learn, but produce different types of offspring
that are well suited or poorly suited for survival in different
environments.  These agents are known as "r-snipes".

* Agents that engage in a simple form of social learning by copying
  from nearby agents ("s-snipes").


## More info

The name of this project comes from the r-snipes' strategy of
producing lots of offspring, many of whom will be maladapted to their
environment and die.  This is like testing pasta by throwing it against
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

My starting points on PEM are Rafal Bogacz, "A tutorial on the free-energy
framework for modelling perception and learning", *Journal of
Mathematical Psychology*, Available online 14 December 2015, ISSN
0022-2496, http://dx.doi.org/10.1016/j.jmp.2015.11.003, and
  Harriet Feldman and Karl Friston, "Attention, Uncertainty, and
  Free-Energy", *Frontiers in Human Neuroscience* 4, 2010,
http://dx.doi.org/10.3389/fnhum.2010.00215.  However, the model
implemented here in the k-snipe agents is very simple and very
different.

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

There are two contiguous environments.  In the left side hand
environment, small mushrooms are nutrious (yellow) and large mushrooms
are poisonous (gray-brown-green).  In the right side environment, large
mushrooms are nutritious and small mushrooms are poisonous.

Snipes gain energy from eating nutritious mushrooms, and lose energy
from eating poisonous mushrooms.  Snipes that accumulate sufficient
energy give birth, and lose energy as a result of the birth.  All
newborn snipes are placed in a random location at birth.

k-snipes (red circles) initially eat mushrooms randomly, but learn to
eat mushrooms whose size (normally distributed) signal indicates that
they are probably nutrious.

r-snipes (blue triangles) never learn.  They produce
offspring that exhibit developmental differences: Roughly half of any
snipe's offspring (squares) always prefer large mushrooms; the others
(triangles) always prefer small mushrooms.  Those suited to the
environment in which they live tend to survive and reproduce, and those
unsuited to their environment tend to die before reproduction.

s-snipes use a social learning (cultural transmission) strategy known
as "success bias".  This is a form of "model bias" because it's a bias
toward learning from "models" (teachers, influencers) who have certain
properties. An newborn s-snipe (purple squares) look around at nearby
snipes, and copies the current mushroom size preference of whichever
snipe has the most energy.  If there are no nearby snipes, the s-snipe
tries again on the next timestep.  Once it adopts a preference, the
preference never changes.  (For more on cultural transmission biases
see e.g. Richerson and Boyd's <em>Not by Genes Alone</em>, University
of Chicago Press 2006.)

## Installation

This program is written in the [Clojure](https://clojure.org) language
using [MASON](http://cs.gmu.edu/~eclab/projects/mason).  MASON is a
Java library for agent-based modeling.

To install, once you clone this repo:

You need Leiningen (http://leiningen.org).  Then change to the root
directory of the repo, and run 'lein deps'.  This will install the right
version of Clojure, if necessary, along with a number of Clojure
libraries and such that free-agent needs.

You'll also need to download the MASON jar file mason.19.jar (or a later
version, probably), and MASON libraries.tar.gz or libraries.zip file.
Move the MASON jar file into the lib directory under this project's
directory. Unpack the contents of the libraries file into this project
as well.   You may want to get the MASON-associated 3D libs, too, in
order to get rid of a warning message during startup.  Place these
jars in the same places as the others.

## Running

Ways to run free-agent:

(1) Running `src/scripts/gui` will start the GUI version of free-agent.

(2) Running `src/scripts/nogui` will start the command line version.  You
may want to run it with `-?` as argument first to see the possible command
line options.

(3) Start the Clojure REPL with `lein repl`.  Then you can start the
   GUI with some variation on the following Clojure commands:

   (use 'free-agent.UI)
   (def cfg (repl-gui))

The GUI should start up, and you can run the program from there.
However, you can also use the REPL to examine the state of the
program:

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

(4) (TODO: Explain how to run by hand in the REPL without interacting
with the GUI.) 


## License

This software is copyright 2016 by [Marshall
Abrams](http://members.logical.net/~marshall/), and is distributed
under the [Gnu General Public License version
3.0](http://www.gnu.org/copyleft/gpl.html) as specified in the file
LICENSE, except where noted, or where code has been included that was
released under a different license.

##

<a id="WhatsPEMnote" href="#WhatsPEMref"><sup>1</sup></a> Prediction energy minimzation: AKA free-energy minimization, predictive
processing, predictive coding. cf. integral control, mean-field
approximation, variational methods.

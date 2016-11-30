# free-agent

Agent-based simulation with free energy minimization within agents.

## free energy minimzation

AKA prediction-error minimization, predictive processing, predictive
coding. cf. integral control.

(My starting point is this excellent article:
Rafal Bogacz, "A tutorial on the free-energy framework for modelling
perception and learning", *Journal of Mathematical Psychology*,
Available online 14 December 2015, ISSN 0022-2496,
http://dx.doi.org/10.1016/j.jmp.2015.11.003 .)

## MASON

The core of this simulation is written in Clojure.  However, I also use
MASON agent-based modeling Java library.  My intention is that you can
use this software with [MASON](http://cs.gmu.edu/~eclab/projects/mason/)
if you want to have a GUI, but that it's also possible to run it without
MASON.  This means you don't have a GUI, but you'll be able to inspect
the data, write it out, and plot it with Incanter.

## Setup

To use this with MASON, you'll need to download the MASON jar file
mason.19.jar (or a later version, probably), and the MASON
libraries.tar.gz or libraries.zip file.  Move the MASON jar file into
the lib directory under this project's directory. Unpack the contents of
the libraries file into this project as well.

To use this without MASON, you still need to download the MASON jar file,
because I use its MersenneTwisterFast random number generator.
Alternatively, copy [MersenneTwisterFast.java](https://cs.gmu.edu/~sean/research/mersenne/MersenneTwisterFast.java)
from (Sean Luke's website](https://cs.gmu.edu/~sean), place it in
src/java/ec/util, and uncomment this line in project.clj:

    :java-source-paths ["src/java"]


## License

This software is copyright 2016 by [Marshall
Abrams](http://members.logical.net/~marshall/), and is distributed
under the [Gnu General Public License version
3.0](http://www.gnu.org/copyleft/gpl.html) as specified in the file
LICENSE, except where noted, or where code has been included that was
released under a different license.

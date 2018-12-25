# pasta

Marshall Abrams,
<a href="http://members.logical.net/~marshall">http://members.logical.net/~marshall</a>

Sections:
<a href="#overview">Overview</a>,
<a href="#howtorun">How to run it</a>,
<a href="#howitworks">How it works</a>,
<a href="#parameters">Parameters</a>,
<a href="#thingstotry">Things to try</a>,
<a href="#moreinfo">More info</a>,
<a href="#fullinstall">Full installation</a>,
<a href="#license">License</a>


<a name="overview"></a><h2>Overview:</h2>

This is a model of evolutionary competition between different strategies
for dealing with variable environments.  It's an agent-based
simulation<a href="#WhatsABMnote" id="WhatsABMref"><sup>1</sup></a>.
There are three kinds of agents (models of organisms) that can be
deployed:

<p><b>k-snipes:</b> Agents that incorporate a very simple model of
individual learning by prediction error minimization (PEM) in some
agents.  These agents implement one kind of "K-strategy" which, roughly,
prioritizes survival over reproduction.  (I do mean it's a
<em>model</em> of PEM; what these agents do, internally, is much, much
simpler than what's usually meant by PEM.)</p>

<p><b>r-snipes:</b> Agents that don't learn, but produce different types of offspring
that are well suited or poorly suited for survival in different
environments.  These agents implement
one kind of "r-strategy", which roughly prioritize reproduction over
survival.</p>

<p><b>s-snipes:</b> Agents that engage in a simple form of social
learning by copying from nearby agents ("s-snipes").</p>

<p>The point is to compare evolutionary strategies and see which is
selected for under different parameter combinations.</p>


<a name="howtorun"></a><h2>How to run it:</h2>

Download <a
href="http://members.logical.net/~marshall/pasta.jar">pasta.jar</a> to your
computer.  If your computer is configured appropriately, you should be
able to double-click or click on this file to run pasta.  If not, and
you're comfortable with a terminal window (OSX terminal, Windows
cmd.exe, any shell window in Linux), you can run pasta using this
command:

    java -jar pasta.jar

If you're not in the directory where you've put pasta.jar, you might
have to add a path to the filename.  You can use command line options to
run pasta in no-GUI mode by executing the line above and adding `--help`.

For other ways to run pasta, see "Full installation" below.










<a name="howitworks"></a><h2>How it works:</h2>

(I won't document features of the GUI that I think are
easy to figure out with a bit of guessing plus trial and error.
Let me know if I'm wrong!)

<p><b>The two environments:</b> Snipes eat mushrooms in one of two
environments.  In the <em>east</em> (usually left) environment, small
mushrooms are nutritious (gray-brown-green) and large mushrooms are
poisonous (greenish yellow).  In the <em>west</em> (usually right)
environment, large mushrooms are nutritious (dark gray) and small
mushrooms are poisonous (light gray).</p>

<p><b>Basic snipe behavior:</b> Snipes move randomly within an
environment, gain energy from eating nutritious mushrooms, and lose
energy from eating poisonous mushrooms.  Snipes that accumulate
sufficient energy give birth to a single offspring, losing energy as a
result of the birth. (one snipe can have multiple offspring if it
repeatedly acquires enough energy to do so.) At birth, each newborn
snipe is placed at a random location in a randomly chosen environment
(as if parents had temporarily migrated to a new location to give
birth).</p>

<p><b>k-snipes</b> (red circles with pointers) initially eat mushrooms
randomly,  but learn to eat mushrooms whose size signal (which is
normally distributed) indicates that they are probably nutritious. The
direction of a k-snipe's pointer&mdash;how far up or down it is
pointing&mdash;indicates the degree of the snipe's preference for large or
small mushrooms.  For details, see doc/kSnipePerception.pdf at
https://github.com/mars0i/pasta.</p>

<p><b>r-snipes</b> (blue triangles) never learn.  They produce
offspring that exhibit developmental differences: Roughly half of any
snipe's offspring (upward-pointing triangles) always prefer large
mushrooms; the others (downward-pointing triangles) always prefer
small mushrooms.  Those suited to the environment in which they live
tend to survive and reproduce, and those unsuited to their environment
generally die before reproduction. </p>

<p><b>s-snipes</b> (purple wing shapes) use a social learning or
cultural transmission strategy known as success bias.  A newborn s-snipe
examines nearby snipes and copies the current mushroom size preference
of whatever nearby snipe has the most energy.  If there are no snipes
that are sufficiently near (see the parameter list below), the s-snipe tries
again on the next timestep.  Once an s-snipe adopts a preference, the
preference never changes.  The direction in which an s-snipe points&mdash;how
far up or down it is tilted&mdash;indicates the degree of the snipe's
preference for large or small mushrooms.</p>

<p><b>Colors:</b> Snipes' energy levels are reflected in their
brightness, with greater brightness indicating more energy.  (This
effect can be subtle.)  The two mushroom colors in each environment
indicate nutritiousness/poisonousness: Darker colors indicate
nutritiousness&mdash;i.e. these mushrooms are energetically favorable to
snipes&mdash;while lighter colors indicate poisonousness&mdash;energetically
unfavorability.  (The fact that the East and West mushrooms have
different hues no functional meaning; this could be considered a
difference between mushroom species, for example.) </p>

<p><b>Monitoring individual snipes:</b> If you pause a run, you can
double-click on a snipe to monitor its internal state and watch it move.
It will be circled so that you can keep track of it.  You can use the
Detach button to monitor multiple snipes at the same time.  If you
double-click on the little magnifying glass next to energy, for example,
and choose chart, you'll open a plot of that snipe's energy level over
time.</p>

<p><b>Overlapping environment display:</b> It's possible to display both
environments overlapping in the same window, using the "overlapping
subenvs" item on the Displays tab.  Then you can think of snipes that
only eat mushrooms of a given color as having a random developmental
restriction to eating those mushrooms.</p>

<p><b>Command line:</b> You can also run pasta from a command line (see
online documents or README.md in the full distribution).  In that case,
running it with "--help" will show options for running pasta.</p>

<a name="parameters"></a><h2>Parameters:</h2>

The following parameters for model runs can be set in the Model tab:

<table style="width:100%">
<tr><td valign=top>NumKSnipes:</td> <td>Initial number of k-snipes.</td></tr>
<tr><td valign=top>NumRSnipes:</td> <td>Initial number of r-snipes.</td></tr>
<tr><td valign=top>NumSSnipes:</td> <td>Initial number of s-snipes.</td></tr>
<tr><td valign=top>MushProb:</td> <td>Average frequency of mushrooms.</td></tr>
<tr><td valign=top>MushLowSize:</td> <td>Snipe-perceptible "size" of small mushrooms (mean of light distribution)</td></tr>
<tr><td valign=top>MushHighSize:</td> <td>Snipe-perceptible "size" of large mushrooms (mean of light distribution)</td></tr>
<tr><td valign=top>MushSd:</td> <td>Standard deviation of mushroom light distribution</td></tr>
<tr><td valign=top>MushPosNutrition:</td> <td>Energy from eating a nutritious mushroom</td></tr>
<tr><td valign=top>MushNegNutrition:</td> <td>Energy from eating a poisonous mushroom</td></tr>
<tr><td valign=top>InitialEnergy:</td> <td>Initial energy for each snipe</td></tr>
<tr><td valign=top>BirthThreshold:</td> <td>Parent energy level at which birth takes place</td></tr>
<tr><td valign=top>KPrefNoiseSd:</td> <td>Standard deviation of internal noise in k-snipe preference determination.</td></tr>
<tr><td valign=top>BirthCost:</td> <td>Energetic cost of giving birth to one offspring</td></tr>
<tr><td valign=top>MaxEnergy:</td> <td>Max energy that a snipe can have.</td></tr>
<tr><td valign=top>CarryingProportion:</td> <td valign=top>Snipes are randomly culled when number exceed this times # of cells.</td></tr>
<tr><td valign=top>NeighborRadius:</td> <td>s-snipe neighbors (for copying) are no more than this distance away.</td></tr>
<tr><td valign=top>ReportEvery:</td> <td>Report basic statistics in shell every this many ticks after the first one (0 = never).</td></tr>
<tr><td valign=top>MaxTicks:</td> <td>Stop model run after this number of timesteps, or never if 0.</td></tr>
<tr><td valign=top>EnvWidth:</td> <td>Width of environments.  Must be an even number.</td></tr>
<tr><td valign=top>EnvHeight:</td> <td>Height of environments. Must be an even number.</td></tr>
<tr><td valign=top>EnvDisplaySize:</td> <td>How large to display environments in GUI by default.</td></tr>
<tr><td valign=top>ExtremePref:</td> <td>Absolute value of r-snipe preferences.</td></tr>
</table>

<p>Some data is reported in other variables displayed in the Model tab:
<table style="width:100%">
<tr><td valign=top>PopsSize</td> <td>Number of snipes in the population (in both environments).</td></tr>
<tr><td valign=top>KSnipeFreq</td> <td>Relative frequency of k-snipes.</td></tr>
<tr><td valign=top>RSnipeFreq</td> <td>Relative frequency of r-snipes.</td></tr>
<tr><td valign=top>SSnipeFreq</td> <td>Relative frequency of s-snipes.</td></tr>
</table>

<p>If you click on the little magnifying glass next to these
elements, there are additional options that MASON provides for
observing their values.</p>

<p>If you run pasta from the command line, (see online documents or "--help"
will show options for setting the parameters listed above, as well as a
few other options concerning whether to display the GUI concerning
writing summary data to a file.</p>

<p>There is additional information at
https://github.com/mars0i/pasta/README.md.</p>

<a name="thingstotry"></a><h2>Things to try:</h2>

Set the number of s-snipes (NumSSnipes) or the number of r-snipes
(NumRSnipes) to zero, so that there is only competition between k-snipes
and one of the other varieties.  Notice that with the default
parameters, k-snipes sometimes increase in frequency early on, but in
the end the r-snipes or s-snipes always increase in frequency at the
expense of k-snipes.  Why?  (Consider monitoring the energy levels of
a few snipes over time.)

<p>Experiment with parameters to see if you can cause k-snipes win the
evolutionary race.  For example, before the start of a k-snipe vs.
r-snipe run, reduce MushNegNutrition.  Does this affects k-snipes'
relative success?  Why?</p>


<a name="moreinfo"></a><h2>More info:</h2>

The name of this project comes from the r-snipes' strategy of
producing lots of offspring, many of whom will be maladapted to their
environment and die.  Kind of like testing pasta by throwing it against
the wall to see what sticks.

k-snipes implement a certain kind of evolutionary  <a
href="http://www.oxfordreference.com/view/10.1093/acref/9780199766444.001.0001/acref-9780199766444-e-3642?rskey=KrGgBD&result=1">K
strategy</a>, while r-snipes implement a certain kind of evolutionary <a
href="http://www.oxfordreference.com/view/10.1093/acref/9780199766444.001.0001/acref-9780199766444-e-6006?rskey=XfTY4o&result=1">r
strategy</a>.  (These terms are not always defined in precisely the same
way.)

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


<a name="fullinstall"></a><h2>Full installation</h2>

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

Ways to run pasta using the full installation:

These instructions were tested on MacOS, and they should work on any
full-fledged unix system such as Linux, MacOS, etc.  The same
instructions might work on Windows, though you might need to substitute
"\" for "/".

(1) From the repository directory execute `src/scripts/gui` or
`src/scripts/pasta` will start the GUI version of pasta.

(2) Running `src/scripts/nogui`, or running `src/scripts/pasta` with
no-GUI command line options will start the command line version (unless
`-g` is specified).  You may want to run it with `-?` as argument first
to see the possible command line options.

(3) Running `src/scripts/repl` will start the Clojure REPL.  This is the
same as running `lein repl`, but displays some helpful suggestions.
e.g. it suggests that you can start the GUI using some variation on the
following Clojure commands:

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


<a name="license"></a><h2>License:</h2>

This software is copyright 2016, 2017 by [Marshall
Abrams](http://members.logical.net/~marshall/), and is distributed
under the [Gnu General Public License version
3.0](http://www.gnu.org/copyleft/gpl.html) as specified in the file
LICENSE, except where noted, or where code has been included that was
released under a different license.

## footnotes:

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

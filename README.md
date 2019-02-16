# pasta

Marshall Abrams,
<a href="http://members.logical.net/~marshall">http://members.logical.net/~marshall</a>

Sections:
<a href="#overview">Overview</a>,
<a href="#howtorun">How to run it</a>,
<a href="#howitworks">How it works</a>,
<a href="#parameters">Parameters &amp; command line options</a>,
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
individual learning by prediction error minimization<a
href="#WhatsPEMnote" id="WhatsPEMref"><sup>2</sup></a> (PEM) in some
agents.  These agents implement one kind of "K-strategy" which,
roughly, prioritizes survival over reproduction.  (I do mean it's a
<em>model</em> of PEM; what these agents do, internally, is much, much
simpler than what's usually meant by PEM.)</p>

<p><b>r-snipes:</b> Agents that don't learn, but produce different
types of offspring that are well suited or poorly suited for survival
in different environments.  These agents use "bet hedging" to
implement one kind of "r-strategy" which, roughly, prioritizes
reproduction over survival, "betting" on the possibilities of
different environmental states.</p>

<p><b>s-snipes:</b> Agents that engage in a simple form of social
learning by copying from nearby agents.</p>

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

(This doesn't document features of the GUI that are probably
easy to figure out with a bit of guessing and trial and error.)

<p><b>The two environments:</b> Snipes eat mushrooms.<a
id="mushjokeref" href="#mushjoke"><sup>3</sup></a> In the
<em>east</em> (usually left) environment, small mushrooms are
nutritious (gray-brown-green) and large mushrooms are slightly
poisonous (greenish yellow).  In the <em>west</em> (usually right)
environment, it's the large mushrooms that are nutritious (dark gray),
and the small mushrooms that are poisonous (light gray).  The general
rule is that darker mushrooms are nutritious and lighter  mushrooms
are poisonous.</p> 

<p><b>Basic snipe behavior:</b> Snipes move randomly within an
environment.  Note that the orientation of snipes' icons does not
indicate direction of movement (see below).  Snipes gain energy from
eating nutritious mushrooms, and lose energy from eating poisonous
mushrooms or from producing an offspring. Movement and internal
processes have no cost.  Snipes that accumulate sufficient energy will
give birth to a single offspring, losing energy as a result. A snipe can
have multiple offspring only by repeatedly acquiring enough energy to
give birth.  At birth, each newborn snipe is placed at a random location
in a randomly chosen environment, as if parents had temporarily migrated
to a new location to leave an egg to hatch.</p>

<p><b>k-snipes</b> (red circles with pointers) initially eat mushrooms
randomly, i.e. with no preference for large or small mushrooms, but
learn to eat mushrooms whose size signal (which is normally distributed)
indicates that they are probably nutritious. The direction of a
k-snipe's pointer&mdash;how far up or down it is
pointing&mdash;indicates the degree of the snipe's preference for large
or small mushrooms.  For details, see <a
href="doc/kSnipePerception.pdf">doc/kSnipePerception.pdf</a>.

<p><b>r-snipes</b> (blue triangles) never learn.  They produce
offspring that exhibit developmental differences: Roughly half of any
snipe's offspring (upward-pointing triangles) always prefer large
mushrooms; the others (downward-pointing triangles) always prefer
small mushrooms.  Those suited to the environment in which they live
tend to survive and reproduce, and those unsuited to their environment
generally die before reproduction.  This is a kind of {\em bet
hedging\/} strategy: Rather than "betting" on a condition (that large
mushrooms are nutritious, for example), r-snipes hedge their bets by
placing bets (in the form of offspring) on both environmental
patterns.</p>

<p><b>s-snipes</b> (purple wing shapes) use a social learning or
cultural transmission strategy known as success bias.  A newborn s-snipe
examines nearby snipes and copies the current mushroom size preference
of whichever nearby snipe has the most energy.  If there are no snipes
that are sufficiently near (see the parameter list below), the s-snipe tries
again on the next time step.  Once an s-snipe adopts a preference, the
preference never changes.  The direction in which an s-snipe points&mdash;how
far up or down it is tilted&mdash;indicates the degree of the snipe's
preference for large or small mushrooms.</p>

<p><b>Colors and energy:</b> Snipes' energy levels are reflected in their
brightness, with greater brightness indicating more energy.  (This
effect can be subtle.)  The two mushroom colors in each environment
indicate nutritiousness/poisonousness: Darker colors indicate
nutritiousness&mdash;i.e. these mushrooms are energetically favorable to
snipes&mdash;while lighter colors indicate poisonousness&mdash;energetic
unfavorability.  (The fact that the East and West mushrooms have
different hues has no functional meaning; it could be considered a
difference between local mushroom species.)</p>

<p><b>More on energy:</b> In this simple model the only things that
can reduce the energy of a snipe are (a) eating poisonous mushrooms,
or (b) producing offspring.  There is no cost to movement or simple
persistence.  You can assume that snipes have another source of
nutrition that maintains them, but that is not represented in the
model. In theory a snipe could live forever without eating any
nutritious mushrooms, as long as it never ate poisonous mushrooms. 
(Such a snipe would never give birth more than once, either; if it had
enough energy to give birth it would, but its energy would be reduced
as a result and it would never acquire any additional energy since it
never ate nutrious mushrooms.) Assume that this other source of
nutritiou would not on its own allow a snipe to gain sufficient energy
for reproduction.  Also, although in nature, mechanisms that allow
learning can be costly in energy use or extra developmental time,
k-snipe's individual learning and s-snipe's social learning has no
energetic or time cost in the model.  k-snipes do suffer a cost
relative to r-snipes, because during a k-snipe's learning process, it
often ends up eating a number of poisonous mushrooms.  This slows down
its acquisition of energy for birth, and sometimes leads to a
sustained loss of energy during its initial learning period. 
(A k-snipe never stops learning, but after a while it will mostly choose
nutritious mushrooms, and its preference for whatever mushroom size is
nutritious in its environment is merely reinforced.)  Whether an
s-snipe's social learning ends up being costly or profitable depends
on its neighbors early in its life.</p>

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

<a name="parameters"></a><h2>Parameters &amp; command line options:</h2>

### Parameters in the GUI:

The following parameters for model runs can be set in the Model tab:

<table style="width:100%"><tr><td valign=top>NumKSnipes:</td> <td>Size of k-snipe subpopulation</td></tr>
<tr><td valign=top>NumRSnipes:</td> <td>Size of r-snipe subpopulation</td></tr>
<tr><td valign=top>NumSSnipes:</td> <td>Size of s-snipe subpopulation</td></tr>
<tr><td valign=top>MushProb:</td> <td>Average frequency of mushrooms.</td></tr>
<tr><td valign=top>MushHighSize:</td> <td>Size of large mushrooms (mean of light distribution)</td></tr>
<tr><td valign=top>MushLowSize:</td> <td>Size of small mushrooms (mean of light distribution)</td></tr>
<tr><td valign=top>MushSd:</td> <td>Standard deviation of mushroom light distribution</td></tr>
<tr><td valign=top>MushPosNutrition:</td> <td>Energy from eating a nutritious mushroom</td></tr>
<tr><td valign=top>MushNegNutrition:</td> <td>Energy from eating a poisonous mushroom</td></tr>
<tr><td valign=top>InitialEnergy:</td> <td>Initial energy for each snipe</td></tr>
<tr><td valign=top>BirthThreshold:</td> <td>Energy level at which birth takes place</td></tr>
<tr><td valign=top>KPrefNoiseSd:</td> <td>Standard deviation of internal noise in k-snipe preference determination.</td></tr>
<tr><td valign=top>BirthCost:</td> <td>Energetic cost of giving birth to one offspring</td></tr>
<tr><td valign=top>MaxEnergy:</td> <td>Max energy that a snipe can have.</td></tr>
<tr><td valign=top>Lifespan:</td> <td>Each snipe dies after this many timesteps.</td></tr>
<tr><td valign=top>CarryingProportion:</td> <td>Snipes are randomly culled when number exceed this times # of cells in a subenv (east or west).</td></tr>
<tr><td valign=top>NeighborRadius:</td> <td>s-snipe neighbors (for copying) are no more than this distance away.</td></tr>
<tr><td valign=top>EnvWidth:</td> <td>Width of env.  Must be an even number.</td></tr>
<tr><td valign=top>EnvHeight:</td> <td>Height of env. Must be an even number.</td></tr>
<tr><td valign=top>ExtremePref:</td> <td>Absolute value of r-snipe preferences.</td></tr>
<tr><td valign=top>ReportEvery:</td> <td>Report basic stats every i ticks after the first one (0 = never); format depends on -w.</td></tr>
<tr><td valign=top>KMaxPopSizes:</td> <td>Comma-separated times and target subpop sizes to cull k-snipes to, e.g. "time,size,time,size"</td></tr>
<tr><td valign=top>RMaxPopSizes:</td> <td>Comma-separated times and target subpop sizes to cull r-snipes to, e.g. "time,size,time,size"</td></tr>
<tr><td valign=top>SMaxPopSizes:</td> <td>Comma-separated times and target subpop sizes to cull s-snipes to, e.g. "time,size,time,size"</td></tr>
<tr><td valign=top>KMinPopSizes:</td> <td>Comma-separated times and target subpop sizes to increase k-snipes to, e.g. "time,size,time,size"</td></tr>
<tr><td valign=top>RMinPopSizes:</td> <td>Comma-separated times and target subpop sizes to increase r-snipes to, e.g. "time,size,time,size"</td></tr>
<tr><td valign=top>SMinPopSizes:</td> <td>Comma-separated times and target subpop sizes to increase s-snipes to, e.g. "time,size,time,size"</td></tr>
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

### Command line options:

<p>If you run pasta from the command line `-?` or `--help` will show
options for setting the parameters listed above, as well as a few other
options concerning whether to display the GUI concerning writing summary
data to a file.  It's also useful to run pasta with `-help` (one dash)
to see additional command line options provided by the MASON library
that pasta uses.   One particularly useful MASON options is `-for
<integer>`, which specifies the number of time steps to run.  Without
`-for` (or `-until`), pasta will run forever, unless you suspend it or
kill it.</p>

Here is more information about making pasta generate data:

If you add `-i <integer>` or `--report-every <integer>` to the command
line, pasta will output summary statistics on different classes of
snipes every `<integer>` timesteps.  If you also add `-w` or
`--write-csv`, pasta will write these statistics to a file.  It will
also write a separate file containing the parameters for this run.  If
you add `-F <name>` or `--csv-basename <name>` as well, pasta will use
`<name>` as the beginning of the filenames.  Stats are also written on
the last timestep, whether it's a multiple of the value for `-i` or
`--report-every`, as long as that value is greater than zero.  This
means that if you only want stats on the last time step, simply give
`-i` or `--report-every` a value greater than the one given to `-for`.

If the data is written to the file, the resulting csv file (which can be
pulled into Excel, for example) will consist of rows of data separated
by commas.  There will be a header row in a separate file with "header"
in its name which you can concatenate onto the data file.  The columns
one for the run id (which is also the random seed), so that you can
append multiple runs to the same file; the time step at which the data
was collected; the snipe class&mdash;i.e. whether it is a k-snipe,
r-snipe, or s-snipe; the sub-environment (east or west) of the snipes
that are summarized; whether the snipes summarized have a negative,
positive, or neutral size preference for mushrooms; the number of
mushrooms in the condition specified by the previous columns; their
average energy; average mushroom preference value; and average age.  If
you use the same basename as an existing data file, or if you specify a
basename with multiple runs using MASON's `-repeat`, the data will be
appended to the file, and a single parameters file will be written;
otherwise, every run's data will have unique filenames.  (Note: A
snipe's mushroom preference value is a real number that is zero to
indicate that the snipe has no preference for large vs. small mushrooms,
positive to indicate the degree of preference for large mushrooms, and
negative to indicate the degree of preference for small mushrooms. See
<a href="doc/kSnipePerception.pdf">doc/kSnipePerception.pdf</a> for
details.)

If the data isn't written to a file, it will be sent to standard output.
The format for the data sent to is different though.  It's not it's not
ideally user-friendly, and I need to document it better.  (The format is
defined somewhere in the guts of stats.clj.) If you prefer the csv
format but want to see data immediately as it's generated, you can write
it to a file and then keep looking at that file; on Unix systems, the
`tail` command is useful for this purpose.

(If you want more fine-grained data, you could modify the source code for
pasta&mdash;I would be happy to help you if I have time&mdash;but I would suggest
simply using the recorded seed to run the same simulation in the GUI.
You can use the GUI to inspect individual snipes that way, as indicated
above.)


<a name="thingstotry"></a><h2>Things to try:</h2>

Set the number of s-snipes (NumSSnipes) or the number of r-snipes
(NumRSnipes) to zero, so that there is only competition between k-snipes
and one of the other varieties.  Notice that with the default
parameters, k-snipes sometimes increase in frequency early on, but in
the end the r-snipes or s-snipes always increase in frequency at the
expense of k-snipes.  Why?  (Consider monitoring the energy levels of
a few snipes over time.)

<p>Experiment with parameters to see if you can cause k-snipes to win the
evolutionary race.  For example, before the start of a k-snipe vs.
r-snipe run, reduce MushNegNutrition.  Does this affects k-snipes'
relative success?  Why?</p>


<a name="moreinfo"></a><h2>More info:</h2>

The name of this project comes from the r-snipes' strategy of
producing lots of offspring, many of whom will be maladapted to their
environment and die.  Kind of like testing pasta by throwing it against
the wall to see what sticks.

As noted above, k-snipes implement a certain kind of evolutionary  <a
href="http://www.oxfordreference.com/view/10.1093/acref/9780199766444.001.0001/acref-9780199766444-e-3642?rskey=KrGgBD&result=1">K
strategy</a>, while r-snipes implement a certain kind of evolutionary <a
href="http://www.oxfordreference.com/view/10.1093/acref/9780199766444.001.0001/acref-9780199766444-e-6006?rskey=XfTY4o&result=1">r
strategy</a>.  (These terms are not always defined in precisely the same
ways.)

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

### Installation:

First clone this git repo.  (If you're not sure how to do this, you
should be able to find beginner info about git and github on the web or
in books written about them.)

You need install Leiningen (http://leiningen.org).  Then change to the
root directory of the repo, and run 'lein deps'.  This will install the
right version of Clojure, if necessary, along with a number of Clojure
libraries and such that pasta needs.

You'll also need to download the MASON jar file mason.19.jar (or a later
version, probably), and MASON libraries.tar.gz or libraries.zip file.
Move the MASON jar file into the lib directory under this project's
directory. Unpack the contents of the libraries file into this directory
as well.   You may want to get the MASON-associated 3D libs, too, in
order to get rid of a warning message during startup.  Place these
jars in the same places as the others.

### Ways to run pasta using the full installation:

There is another way to run it described above in "How to run it".

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

This software is copyright 2016, 2017, 2018, 2019 by [Marshall
Abrams](http://members.logical.net/~marshall/), and is distributed
under the [Gnu General Public License version
3.0](http://www.gnu.org/copyleft/gpl.html) as specified in the file
LICENSE, except where noted, or where code has been included that was
released under a different license.

## footnotes:

<a id="WhatsABMnote" href="#WhatsABMref"><sup>1</sup></a>
"Agent-based" here refers to the loose class of <a
href="https://en.wikipedia.org/wiki/Agent-based_model">simulations</a>
in which outcomes of interest come from interactions between many
semi-independent entities&mdash;*agents*&mdash;which often model
behaviors or interactions between people, organisms, companies, etc. 
"Agent-based" does *not* refer to various ways of dealing with
concurrency, as for example with Clojure's <a
href="https://clojure.org/reference/agents">agent</a> data structure.

<a id="WhatsPEMnote" href="#WhatsPEMref"><sup>2</sup></a> Prediction
energy minimization: AKA free-energy minimization, predictive
processing, predictive coding. cf. integral control, mean-field
approximation, variational methods.

<a id="mushjoke" href="#mushjokeref"><sup>3</sup></a>Yes&mdash;this
pasta has mushrooms in it.  Yum!

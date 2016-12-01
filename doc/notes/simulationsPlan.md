simulationsPlan.md
====

There are some quotations below re choice of animal names, etc.


. Every animal has an energy value.  Eating a nutrious mushroom
increase the energy value.  


. There are two habitats, Left and Right (which is where they appear
on the screen).  Each has both large and small mushrooms, but in one,
the large mushrooms and poisonous, and the small are not, and in the
other habitat, the reverse is true.


. There are two genetic variants, R and K.


. There is a spawning ground, which is either a line in the center
between the two habitats, or is a point in the middle of the screen.
Animals are born there and then move away in a random direction.
Maybe the spawning ground is a mountain peak or ridge.
(Or: Animals are born in random locations.)


. Every patch is a mushroom patch. There are poison mushroom patches and
nutrious mushroom patches.


. R animals are of two types.  Obligate large-mushroom eaters and
obligate small-mushroom eaters.  This is due to random developmental
differences.  (Or there are two lineages?)

Question: Is due to a noisy signal?  Or are R's perfect perceivers?

Sure--let it be noisy.  Then R's can make mistakes when the signal is
an outlier.


. K animals move to a patch, look at the mushrooms quickly, and decide
whether to eat based on the signal they receive.

The signal might represent mushroom cap area, or it might represent
some function of an entire mushroom patch.

Yes, suppose that the animals (deer?) actually perceive the patches
accurately, but patch growth and area is noisy, as growth and dispersal
are.


. Signals are randomly distributed around a mean that differs for
large and small mushrooms.


. Young K's have to learn which mushroom size is nutritious.  They eat
the mushrooms in a patch, and if they don't get sick (i.e. if their
energy level goes up and not down), they learn from that fact.  Use,
potentially, m random size signals from that patch as inputs in m
timesteps of the Bogacz algorithm.  m might be a small number such as 1,
so that you have to learn from multiple patches.  Poss algorithms:

Scalar: 
If you don't get sick, use the signal(s); otherwise ignore them.

Vector #1:
What you learn from is vector signal, composed to the size value and
the nutritious/poison value, which is either 0 (poison) or 1
(nutritious).  The task is to learn the size/nutritious correlation,
and then eat only mushrooms whose signals correlate with 1.
Initially there's no correlation, so eating is random.

What's the data structure?
OK, at each patch, the animal generates a 2-element vector, where one
element is 0 or 1 (noxious, tasty), and another element is continuous
(size).  And then sigma will be a 2x2 matrix.

Vector #2:
Same thing, but the nutritious/poison signal is also noisy.


[Ah, I was really liking vector version 1 above, rather than the scalar
cutoff version, but adding an inverse for aljabar is more kludgey and
complex than I expected.  Will have to implement at least a simple
determinant as well as inverse.  Worried that there will be one problem
after another, and not worth it.  So if I don't go down the inverse
kludge route, either I do the scalar version above, which is kind
of funky for publication purposes, or retreat from the idea of
putting the simulation on the web in Agentscript, and use MASON
instead.  Which is OK, but ....  (You know, I could distribute it as a
jar even though I couldn't stick it up in a web page.  But still.)]

(In MASON, can I give the sim non-overlapping "simultaneous" behaviors,
and then just draw from a lazy sequence as in popco?  Maybe have the
MASON sim pull the next element from the sequence at each step?  Or
drive the ABM as side-effects while mapping over the timestep sequence?
Note -start in Sim.clj contains the top-level "loop". Oh, yeah, well
actually that's what I did in Intermittran.  In each timestep I just
doseq through the entire pop three times for three different operations.
So I could do either of the things mentioned above in the same
place--run through the Clojure iteration with side-effects
manipulating MASON, or do an interative MASON update that pulls
the Clojure iteration part along.)

In intermittran, the top-level loop is in Sim/-start. i.e. you init
the system, and then you call scheduleRepeating().  And then that
calls a class Steppable with a method step that calls my single-iteration
loop-through-all-of-the-agents methods.  These are methods in my deftype
Indiv.  i.e. I call these three methods on each indiv when the scheduler
runs the Steppable.  Each of the methods updates fields in the Indiv.
Then there are methods in Indiv and calls in SimWithUI/setup-portrayals
that updates the GUI.  There's some scheduling in SimWithUI too.
I was wondering whether I could drive all of that with e.g. Clojure's take
from a repl, but I'm not sure you can drive the GUI this way.

NOTE even if I can get the model working with Agentscript using
matrices, and even if the Sean Luke's and chance.js's Mersenne Twister's
behave identically with the same seeds, I still can't expect models to
run identically in both Clojure and Clojurescript given the same seed:
Float rounding might differ.  This is a reason to run the whole thing
in Clojure, so that if I have 100 runs, say, and one is odd, I can run
it with the GUI to see what was going on.  If the 100 runs were in
Clojure and the GUI is in Clojurescript, this might not work.


. Question: What determines when/whether eating occurs?

Vector versions:
Um, compute the correlation between tasty and size?  
Doesn't sigma do that?  So I can use that?  
i.e. just look at the (0,0) element of sigma?

And then have a threshold, and iff the correlation is high enough, eat?
But in that case how do you learn what's tasty and noxious?
So maybe eat iff the correlation is positive?
That seems OK.  Since it's gradually changing over time, and once
learning has fully taken place, the correlation should remain high.

Scalar version:
Um, wait until sigma is high enough?


. So the R animals either survive or quickly die.  If they survive,
they quickly acquire a lot of energy, because they eat all and only the
nutrious mushrooms.

K animals, by contrast, have a slower ramp-up as they learn which
mushrooms are nutrious.  During that period, their energy level
fluctuates.


. Reproduction is a function of energy level.  Iff your energy
level gets to a certain level (and maybe after a certain interval of
time, too), a litter is produced and your energy is decremented.  If
your energy level gets too low, you die.


. Litters are the same size for R's, and for all K's.  Maybe R's have
larger litters, but maybe they simply reproduce more often.


. NOTE that the R strategy is a kind of minimal FEM system: There is
one level, and its output is treated as correct, with zero error
comparison, etc.  i.e. the effect of error is scaled to zero.  (I should
present it as such.)


............................

In Agentscript, there's a global step procedure.  In the simple
example I modified, this iterates through the turtles to make them run
individually.

Question: How am I managing with births and deaths?  Remove and conj
onto a sequence?  Or should the agents be stored in a hashmap so that
they can be looked up using 'dissoc' to remove them?

If I use a hash, then I don't have to keep the Agentscript turtles and
the banana slugs or squirrels or deer the same order.  e.g. I could
iterate through the turtles and have each one go and find its squirrel
to know what to display.  Or could I merge the underlying and turtle
data structures? That puts together model and view, which might be
bad, but it means that when there's a death or birth, there are not
two separate collections to keep in sync.

Note that the step procedure in my example Agentscript model iterates
through the turtles in the model instance.  I was imaginging that they
could lookup the underlying animals to find out what to do.  But step
could just as easily iterate through the underlying animals, and then
find the corresponding turtles.  For that, you'd want pointers into
turtles, which presumably is a Javascript array.  Then again these 
pointers would change with births and deaths.  Ugh.  Unless turtles
could be made into an object instead of an array, if that's what it is.
So maybe better to iterate through the turtles, since I have more
control over the Clojure side.


............................

The animals are:
squirrels? snails? banana slugs? (see Wikipedia "fungivore" page
picture), wild pigs?  Wikipedia: Northern flying squirrels? jays?
Emus? (a flightless bird)  deer?

Quotations:

	http://forestforagers.co.uk/faq.html?catid=2:

	A wide range of animals are known to eat wild mushrooms – some
	examples include badgers, deer, mice, pigs, rabbits and
	squirrels. Wild mushrooms are also eaten by slugs, snails and
	many insects including ants and termites which cultivate their
	own fungus gardens. It is dangerous to assume that it is safe
	for humans to eat the same species that animals consume without
	any ill effects – deer and rabbits can eat poisonous fungi with
	impunity.

	https://en.wikipedia.org/wiki/Fungivore:

	Fungi are renowned for their poisons to deter animals from
	feeding on them: even today humans die from eating poisonous
	fungi. A natural consequence of this is the virtual absence of
	obligate vertebrate fungivores. One of the few extant vertebrate
	fungivores is the northern flying squirrel, but it is believed
	that in the past there were numerous vertebrate fungivores and
	that toxin development greatly lessened their number and  forced
	these species to abandon fungi or diversify. Although some
	monkeys still eat fungi today, there are no completely
	fungivorous primates, though their dentition is very suitable
	for eating fungi.

An anecdote:

	https://www.reddit.com/r/askscience/comments/kwg3t/how_do_wild_animals_know_which_mushrooms_are_safe/

	This is more of a case study, but I own a flock of sheep (they
	free range) and the ewes teach their lambs what to eat (ie, the
	lambs only eat what their mothers do). Bum lambs (lambs whose
	mother has abandoned them at birth and that require human
	intervention) get poisoned from grazing much more often.

This article says that one factor that allows deer to each poisonous
substances is simply not eating too much of them.  

	http://www.adfg.alaska.gov/index.cfm?adfg=wildlifenews.view_article&articles_id=249

However, it also explains that some deer have other mechanisms that
allow them to tolerate more.  note this quotation from the same
article:

	In a similar way, microbial adaptations in the gut can be
	induced by consumption of small quantities of plant toxins,
	which can provide an opportunity for the animal’s system to
	adapt to the toxin. This explains why different individuals
	within a species may have better tolerances for some foods.

Maybe I could use deer as the animal, and say that there is a lot of
other non-mushroom eating going on at the same time.

So far, I like deer and emus.  Or snails or slugs, but then it's less
evocative of a complex causal system, and snails and slugs probably
don't use visual perception to identify food, so I'd have to say that
what's being perceived is some noisy smell signal.

	http://nervousbiology.weebly.com/banana-slug.html:

	Typically, most people would say that slugs are very
	underdeveloped and un-evolved. As a matter of fact, on the
	evolutionary scale, Ariolimax is very similar to our own nervous
	system. Banana slugs have highly developed sensory organs- these
	send nerve impulses on clusters of neurons throughout its body.
	This process is basically the same as what happens in our own
	bodies when affected by an exterior stimuli. The central ganglia
	in these slugs function as a primitive brain. Information taken
	from the brain is relayed through nerve impulses to the slug's
	muscles and receptors. The sole nerve cord which runs down the
	length of the slug's body also allows changes in mental
	behavior, as opposed to simply reacting to physical stimuli.
	Again, this is highly similar to the functions which we
	undertake.

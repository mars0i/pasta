Issues 12/2016 about clicking, double clicking,
inspectors, CircledPortrayal2D

Using the latest MASON distribution from the repo, with some
fixed that Sean Luke added recently:

With java 1.8.0_51 on the mba, I could get inspectors from
double-clicking on objects, but CircledPortrayal2D didn't respond to
clicks.

With Java 1.8.0_66 (?) on the mbp, I got the circles when clicking,
but instead of inspectors, I get a NPE.

I upgraded to java 1.8.0_112 on the mba, and now I get the circles but
also the NPE instead of inspectors.  This is true both on the master
branch and on branch non-fnl (but the circle will follow a snipe
only on non-fnl, not surprisingly).

My little java example masonborder gives both circles and inspectors
from double-clicking.

Intermittran gives inspectors when I double-cick (and it's not set up
with CircledPortrayal2D), and in fact they update as it runs.

In my Clojure Defrecord version of the Students example from the
manual, I get everything: The circle, which moves with the node, and
inspectors, which update as it runs.

So the inspector problem is specific to something I'm doing in
free-agent.

Adding a single bean-style accessor to the snipes (getEnergy) doesn't
help.  Hmm.  Surprising--because the NPE is on an arraylist called
`getMethods` in SimpleProperties.java.

I tried adding a line in project.clj that would compile to Java 1.5,
since that's what happens when compiling MASON.  This didn't help.

I tried adding getEnergy with a protocoal and an interface.  No help.


What's different about Intermittran?

1. The agent defs are in the same namespace as the main SimState
subclass.  And the gen-class is bigger.

2. It uses deftype rather than defrecord.  No, I have deftype
in branch non-fnl, so that's not it.

Note that I am importing snipe and all that into free-agent.UI
already.  Adding snipe as a require doesn't help.

I tried adding "-" to getEnergy.  Had to do that in the interface,
too, and got the same error when double-clicking.

aot-compiling free-agent.snipe didn't help.

I tried moving snipe.clj and popenv.clj into SimConfig.clj.
See branches snipes-in-simconfig and non-fnl-in-simconfig.
Nothing!  This didn't help at all.  I get the same NPE on
line 642 in SimplePortrayal2D.java.

So both #2 and #1 above seem to be red herrings.  !

### free-agent stackdump:

Exception in thread "AWT-EventQueue-0" java.lang.NullPointerException
	at sim.util.SimpleProperties.numProperties(SimpleProperties.java:642)
	at sim.portrayal.SimpleInspector.generateProperties(SimpleInspector.java:195)
	at sim.portrayal.SimpleInspector.<init>(SimpleInspector.java:62)
	at sim.portrayal.SimpleInspector.<init>(SimpleInspector.java:93)
	at sim.portrayal.SimpleInspector.<init>(SimpleInspector.java:85)
	at sim.portrayal.Inspector.getInspector(Inspector.java:98)
	at sim.portrayal.SimplePortrayal2D.getInspector(SimplePortrayal2D.java:79)
	at sim.portrayal.FieldPortrayal.getInspector(FieldPortrayal.java:298)
	at sim.display.Display2D.createInspectors(Display2D.java:1744)
	at sim.display.Display2D.createInspectors(Display2D.java:1767)
	at sim.display.Display2D$8.mouseClicked(Display2D.java:1392)
	at java.awt.AWTEventMulticaster.mouseClicked(AWTEventMulticaster.java:269)
	at java.awt.Component.processMouseEvent(Component.java:6536)
	at javax.swing.JComponent.processMouseEvent(JComponent.java:3324)
	at java.awt.Component.processEvent(Component.java:6298)
	at java.awt.Container.processEvent(Container.java:2236)
	at java.awt.Component.dispatchEventImpl(Component.java:4889)
	at java.awt.Container.dispatchEventImpl(Container.java:2294)
	at java.awt.Component.dispatchEvent(Component.java:4711)
	at java.awt.LightweightDispatcher.retargetMouseEvent(Container.java:4888)
	at java.awt.LightweightDispatcher.processMouseEvent(Container.java:4534)
	at java.awt.LightweightDispatcher.dispatchEvent(Container.java:4466)
	at java.awt.Container.dispatchEventImpl(Container.java:2280)
	at java.awt.Window.dispatchEventImpl(Window.java:2746)
	at java.awt.Component.dispatchEvent(Component.java:4711)
	at java.awt.EventQueue.dispatchEventImpl(EventQueue.java:758)
	at java.awt.EventQueue.access$500(EventQueue.java:97)
	at java.awt.EventQueue$3.run(EventQueue.java:709)
	at java.awt.EventQueue$3.run(EventQueue.java:703)
	at java.security.AccessController.doPrivileged(Native Method)
	at java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(ProtectionDomain.java:80)
	at java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(ProtectionDomain.java:90)
	at java.awt.EventQueue$4.run(EventQueue.java:731)
	at java.awt.EventQueue$4.run(EventQueue.java:729)
	at java.security.AccessController.doPrivileged(Native Method)
	at java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(ProtectionDomain.java:80)
	at java.awt.EventQueue.dispatchEvent(EventQueue.java:728)
	at java.awt.EventDispatchThread.pumpOneEventForFilters(EventDispatchThread.java:201)
	at java.awt.EventDispatchThread.pumpEventsForFilter(EventDispatchThread.java:116)
	at java.awt.EventDispatchThread.pumpEventsForHierarchy(EventDispatchThread.java:105)
	at java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:101)
	at java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:93)
	at java.awt.EventDispatchThread.run(EventDispatchThread.java:82)

### masonborder stackdump:

Exception in thread "AWT-EventQueue-0" java.lang.NullPointerException
	at sim.util.SimpleProperties.numProperties(SimpleProperties.java:642)
	at sim.portrayal.SimpleInspector.generateProperties(SimpleInspector.java:195)
	at sim.portrayal.SimpleInspector.<init>(SimpleInspector.java:62)
	at sim.portrayal.SimpleInspector.<init>(SimpleInspector.java:93)
	at sim.portrayal.SimpleInspector.<init>(SimpleInspector.java:85)
	at sim.portrayal.Inspector.getInspector(Inspector.java:98)
	at sim.portrayal.SimplePortrayal2D.getInspector(SimplePortrayal2D.java:79)
	at sim.portrayal.FieldPortrayal.getInspector(FieldPortrayal.java:298)
	at sim.display.Display2D.createInspectors(Display2D.java:1744)
	at sim.display.Display2D.createInspectors(Display2D.java:1767)
	at sim.display.Display2D$8.mouseClicked(Display2D.java:1392)
	at java.awt.AWTEventMulticaster.mouseClicked(AWTEventMulticaster.java:269)
	at java.awt.Component.processMouseEvent(Component.java:6536)
	at javax.swing.JComponent.processMouseEvent(JComponent.java:3324)
	at java.awt.Component.processEvent(Component.java:6298)
	at java.awt.Container.processEvent(Container.java:2236)
	at java.awt.Component.dispatchEventImpl(Component.java:4889)
	at java.awt.Container.dispatchEventImpl(Container.java:2294)
	at java.awt.Component.dispatchEvent(Component.java:4711)
	at java.awt.LightweightDispatcher.retargetMouseEvent(Container.java:4888)
	at java.awt.LightweightDispatcher.processMouseEvent(Container.java:4534)
	at java.awt.LightweightDispatcher.dispatchEvent(Container.java:4466)
	at java.awt.Container.dispatchEventImpl(Container.java:2280)
	at java.awt.Window.dispatchEventImpl(Window.java:2746)
	at java.awt.Component.dispatchEvent(Component.java:4711)
	at java.awt.EventQueue.dispatchEventImpl(EventQueue.java:758)
	at java.awt.EventQueue.access$500(EventQueue.java:97)
	at java.awt.EventQueue$3.run(EventQueue.java:709)
	at java.awt.EventQueue$3.run(EventQueue.java:703)
	at java.security.AccessController.doPrivileged(Native Method)
	at java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(ProtectionDomain.java:80)
	at java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(ProtectionDomain.java:90)
	at java.awt.EventQueue$4.run(EventQueue.java:731)
	at java.awt.EventQueue$4.run(EventQueue.java:729)
	at java.security.AccessController.doPrivileged(Native Method)
	at java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(ProtectionDomain.java:80)
	at java.awt.EventQueue.dispatchEvent(EventQueue.java:728)
	at java.awt.EventDispatchThread.pumpOneEventForFilters(EventDispatchThread.java:201)
	at java.awt.EventDispatchThread.pumpEventsForFilter(EventDispatchThread.java:116)
	at java.awt.EventDispatchThread.pumpEventsForHierarchy(EventDispatchThread.java:105)
	at java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:101)
	at java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:93)
	at java.awt.EventDispatchThread.run(EventDispatchThread.java:82)


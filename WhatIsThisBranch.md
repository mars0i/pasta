This branch fnl-circle was an attempt to get circles to display
continuously on inspected snipes (successful ...), and to turn
of the circle when the snipe was no longer inspected (unsuccessful)
by overriding getInspector in a custom CircledPortrayal2D subclass.
(I subsequently moved the first part of the code into master,
but abandoned the getInspector strategy, instead using the kludge
of allowing user to turn off circle with a settable boolean field.

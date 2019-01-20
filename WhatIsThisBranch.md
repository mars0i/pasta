An attempt to record next snipe id in the Sim instance map variable.
Result is that after the first tick, you end up creating ids
constantly and nothing runs.  Maybe has something to do with that
there is recursive data in the map after the first tick.
Not sure.  I reverted to the earlier version, which means that there
is an atom shared between threads, but should be OK otherwise.

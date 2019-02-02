what is this branch
===

This is branch type-hinted-newval.

It stores an experiment in which defsim.clj type-hinted the newval
parameter of the bean settors.  I thought it would get rid of some
reflection warnings, but it didn't, because the warnings were actually
on the constructors listed at the end of each line in the defsim
specification.  

The problem was that the parameter to the setter actually has (various)
primitive types (long, double), and not the wrapper types used in the
constructors (Long, Double, i.e. java.lang.Long and java.lang.Double).

I don't think I want the wrapper types in the running accessor code.
Seems like that would slow things down.  I don't want to replace the
primitive numeric types with wrappers.

Moreover, the wrapper types are used only in processing commandline
options, I believe, so getting rid of those reflection warnings
would not improve the runtime speed at all in any meaningful way.

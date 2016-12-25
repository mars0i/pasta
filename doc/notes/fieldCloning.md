On cloning a MASON ObjectGrid2D field
====

I don't think `clone` on the field will work.

However, there's a constructors that takes an existing field as
argument.  It does some dimension checks, and then copies a `clone`
of each element into the new field.  (This is not documented in the
classdocs or in the manual afaik; I got it from the source.)

I might be able to make this faster by doing it on my own.

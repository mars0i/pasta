notes on gen-state macro to wrap gen-class, etc.
====

## info needed

1. names
2. types
3. other gen-class stuff

##  Code to generate:

1. Corresponding bean-style and Clojure-style names.  Easier to start 
   with Clojure names and then mangle them into Java names.
3. `default-<Clojure-style>` names to correspond to defs previous in file.
2. gen-class statement, with typed Bean-style methods.
3. defrecord or deftype InstanceState
4. -init-instance-state, which is called by constructor, and
   will include an InstanceState creation with arguments that
   wrap initial values in atoms.  
5. Bean gettor and settor methods

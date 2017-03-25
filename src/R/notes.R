#examples:

df1 <- read.csv(file="KvsR.csv")

# make factors
df1$snipe_class <- factor(df1$snipe_class, order=T, levels=c("k", "r", "s"))
df1$subenv <- factor(df1$subenv, order=T, levels=c("west", "east"))
df1$pref_sign <- factor(df1$pref_sign, order=T, levels=c("pos", "neg", "zero"))

# display factors
levels(df1$pref_sign)
levels(df1$snipe_class)
levels(df1$subenv)

aggregate(count ~ snipe_class * run * step, df1[df1$step==1000,])
df.ag <- aggregate(count ~ snipe_class * run * step, df1, sum)

xyplot(count ~ step | run * snipe_class, data=df1.ag, type=c("l")
xyplot(count ~ step | run * snipe_class, data=df1.ag, pch=".")

r <- xyplot(count ~ step | run, data=df1.ag[df1.ag$snipe_class=="r",], type=c("l"), col="red")
k <- xyplot(count ~ step | run, data=df1.ag[df1.ag$snipe_class=="k",], type=c("l"))
r + as.layer(k) # do it this way rather than other way around because the first one sets the scale, and r goes higher


# a working sequence of commands:
library(latticeExtra)
df3 <- read.csv("KvsS.csv")
df3$run <- factor(df3$run)
df3.ag <- aggregate(count ~ snipe_class * run * step, df3, sum)
s <- xyplot(count ~ step | run, data=df3.ag[df3.ag$snipe_class=="s",], type=c("l"), col="red")
k <- xyplot(count ~ step | run, data=df3.ag[df3.ag$snipe_class=="k",], type=c("l"))
s + as.layer(k)



dfr <- read.csv(file="KvsR.csv")
dfr.ag <- aggregate(count ~ snipe_class * run * step, dfr, sum) # sum counts within snipe classes
rwp <- xyplot(count ~ step | run, data=dfr.ag[dfr.ag$subenv=="west" & dfr.ag$snipe_class=="r" & dfr.ag$pref_dir=="pos",], type=c("l"), col="green")
rwn <- xyplot(count ~ step | run, data=dfr.ag[dfr.ag$subenv=="west" & dfr.ag$snipe_class=="r" & dfr.ag$pref_dir=="neg",], type=c("l"), col="red")
rwz <- xyplot(count ~ step | run, data=dfr.ag[dfr.ag$subenv=="west" & dfr.ag$snipe_class=="r" & dfr.ag$pref_dir=="zero",], type=c("l"), col="blue")
rwp + as.layer(rwn) + as.layer(rwz)
rep <- xyplot(count ~ step | run, data=dfr.ag[dfr.ag$subenv=="east" & dfr.ag$snipe_class=="r" & dfr.ag$pref_dir=="pos",], type=c("l"), col="green")
ren <- xyplot(count ~ step | run, data=dfr.ag[dfr.ag$subenv=="east" & dfr.ag$snipe_class=="r" & dfr.ag$pref_dir=="neg",], type=c("l"), col="red")
rez <- xyplot(count ~ step | run, data=dfr.ag[dfr.ag$subenv=="east" & dfr.ag$snipe_class=="r" & dfr.ag$pref_dir=="zero",], type=c("l"), col="blue")


# Expanding on an idea at http://stackoverflow.com/a/9723314/1455243, I thought this might work (it doesn't):
agger <- function(x){count <- x[1]; energy <- x[2]; pref <- x[3]; age <- x[4]; newcount <- sum(count); newenergy <- sum(count * energy)/newcount;   newpref <- sum(count * pref)/newcount; newage <- sum(count * age)/newcount; c(newcount, newenergy, newpref, newage)}
aggregate(cbind(count, energy, pref, age) ~ snipe_class * run * step, kvsr, agger)


###########################################################################

# I *think* this shows that among r-snipes, pop sizes in the west were smaller
# on average than those in the east.

# If so, Why?  
# Lines 366ff in popenv.clj are where newborns' field is chosen.
# Could there a different wrt mushrooms?

# r-snipes:
kvsr.subenvs <- aggregate(count ~ snipe_class * run * step * subenv, kvsr, sum)
west <- kvsr.subenvs[kvsr.subenvs$snipe_class=="r" & kvsr.subenvs$subenv=="west",]
east <- kvsr.subenvs[kvsr.subenvs$snipe_class=="r" & kvsr.subenvs$subenv=="east",]
sortwest <- west[with(west, order(run, snipe_class, step)),]
sorteast <- east[with(east, order(run, snipe_class, step)),]
sum(sorteast$count - sortwest$count)
[1] 42636
summary(sortwest$count - sorteast$count)  # note order is swapped
   Min. 1st Qu.  Median    Mean 3rd Qu.    Max. 
-58.000  -9.000  -3.000  -4.264   2.000  50.000 

## And the opposite is true of the k's competing against them:
west <- kvsr.subenvs[kvsr.subenvs$snipe_class=="k" & kvsr.subenvs$subenv=="west",]
east <- kvsr.subenvs[kvsr.subenvs$snipe_class=="k" & kvsr.subenvs$subenv=="east",]
sortwest <- west[with(west, order(run, snipe_class, step)),]
sorteast <- east[with(east, order(run, snipe_class, step)),]
sum(sortwest$count - sorteast$count)
[1] 15522
summary(sortwest$count - sorteast$count)
   Min. 1st Qu.  Median    Mean 3rd Qu.    Max. 
-39.000  -3.000   1.000   1.552   6.000  36.000 

# now for K vs S:
kvss.subenvs <- aggregate(count ~ snipe_class * run * step * subenv, kvss, sum)
west <- kvss.subenvs[kvss.subenvs$snipe_class=="s" & kvss.subenvs$subenv=="west",]
east <- kvss.subenvs[kvss.subenvs$snipe_class=="s" & kvss.subenvs$subenv=="east",]
sortwest <- west[with(west, order(run, snipe_class, step)),]
sorteast <- east[with(east, order(run, snipe_class, step)),]
# The S case looks like chance:
sum(sortwest$count - sorteast$count)
[1] -881
summary(sortwest$count - sorteast$count)
     Min.   1st Qu.    Median      Mean   3rd Qu.      Max. 
-146.0000  -12.0000   -2.0000   -0.4405    7.0000  161.0000 

# The K case:
west <- kvss.subenvs[kvss.subenvs$snipe_class=="k" & kvss.subenvs$subenv=="west",]
east <- kvss.subenvs[kvss.subenvs$snipe_class=="k" & kvss.subenvs$subenv=="east",]
sortwest <- west[with(west, order(run, snipe_class, step)),]
sorteast <- east[with(east, order(run, snipe_class, step)),]
# Also looks like chance:
sum(sortwest$count - sorteast$count)
[1] -291
summary(sortwest$count - sorteast$count)
    Min.  1st Qu.   Median     Mean  3rd Qu.     Max. 
-35.0000  -5.0000  -1.0000  -0.1455   4.0000  33.0000 

# So if there's a worry, it's more with K vs R.

# here's how I made RblueKredEastWestDiffs.pdf:
...
> west <- kvsr.subenvs[kvsr.subenvs$snipe_class=="r" & kvsr.subenvs$subenv=="west",]
> east <- kvsr.subenvs[kvsr.subenvs$snipe_class=="r" & kvsr.subenvs$subenv=="east",]
> sortwest <- west[with(west, order(run, snipe_class, step)),]
> sorteast <- east[with(east, order(run, snipe_class, step)),]
> 
> eastwest <- sortwest - sorteast
Warning messages:
1: In Ops.factor(left, right) : ‘-’ not meaningful for factors
2: In Ops.factor(left, right) : ‘-’ not meaningful for factors
3: In Ops.factor(left, right) : ‘-’ not meaningful for factors
> class(sortwest)
[1] "data.frame"
> head(sortwest)
      snipe_class       run step subenv count
20002           r -72727835   10   west    26
20102           r -72727835   20   west    25
20202           r -72727835   30   west    26
20302           r -72727835   40   west    26
20402           r -72727835   50   west    26
20502           r -72727835   60   west    25
> eastwest <- sortwest
> eastwest$count <- sortwest$count - sorteast$count
> head(eastwest)
      snipe_class       run step subenv count
20002           r -72727835   10   west     2
20102           r -72727835   20   west     2
20202           r -72727835   30   west     3
20302           r -72727835   40   west     4
20402           r -72727835   50   west     3
20502           r -72727835   60   west     2
> xyplot(count ~ step | run, data=eastwest type=c("l"), col="blue", layout=c(10,5))
Error: unexpected symbol in "xyplot(count ~ step | run, data=eastwest type"
> xyplot(count ~ step | run, data=eastwest, type=c("l"), col="blue", layout=c(10,5))
> r <- xyplot(count ~ step | run, data=eastwest, type=c("l"), col="blue", layout=c(10,5))
> r.eastwest <- eastwest
> xyplot(count ~ step | run, data=eastwest, type=c("l"), col="blue", layout=c(10,5))
> west <- kvsr.subenvs[kvsr.subenvs$snipe_class=="k" & kvsr.subenvs$subenv=="west",]
> east <- kvsr.subenvs[kvsr.subenvs$snipe_class=="k" & kvsr.subenvs$subenv=="east",]
> sortwest <- west[with(west, order(run, snipe_class, step)),]
> sorteast <- east[with(east, order(run, snipe_class, step)),]
> 
> k.eastwest <- eastwest
> k.eastwest <- sortwest
> k.eastwest$count <- sortwest$count - sorteast$count
> head(k)
Error in head(k) : object 'k' not found
> head(k.eastwest)
      snipe_class       run step subenv count
20001           k -72727835   10   west     3
20101           k -72727835   20   west     2
20201           k -72727835   30   west     1
20301           k -72727835   40   west     2
20401           k -72727835   50   west     2
20501           k -72727835   60   west     1
> k <- xyplot(count ~ step | run, data=k.eastwest, type=c("l"), col="red", layout=c(10,5))
> r + as.layer(k)
> 


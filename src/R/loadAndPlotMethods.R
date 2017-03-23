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

k <- xyplot(count ~ step | run, data=df1.ag[df1.ag$snipe_class=="k",], type=c("l"))
r <- xyplot(count ~ step | run, data=df1.ag[df1.ag$snipe_class=="r",], type=c("l"), col="red")
r + as.layer(k) # do it this way rather than other way around because the first one sets the scale, and r goes higher


# a working sequence of commands:
library(latticeExtra)
df3 <- read.csv("KvsS.csv")
df3$run <- factor(df3$run)
df3.ag <- aggregate(count ~ snipe_class * run * step, df3, sum)
r <- xyplot(count ~ step | run, data=df3.ag[df3.ag$snipe_class=="s",], type=c("l"), col="red")
k <- xyplot(count ~ step | run, data=df3.ag[df3.ag$snipe_class=="k",], type=c("l"))
r + as.layer(k)

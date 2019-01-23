kvsr7501 <- read.csv("KvsR_birthcost75_negnut01.csv")
kvsr7501.ag <- aggregate(count ~ snipe_class * run * step, kvsr7501, sum)
kvsr7501.ag$step <- factor(kvsr7501.ag$step)
save(kvsr7501, kvsr7501.ag, file="KvsR_birthcost75_negnut01.rdata")
bwplot(count ~ snipe_class | step, data=kvsr7501.ag, main="k-snipe, r-snipe pop sizes across 100 runs\nbirth cost = 7.5, neg nutrition = -0.1", xlab="snipe variety", layout=c(5,1))
bwplot(count ~ snipe_class | step, data=kvsr7501.ag, main="k-snipe, r-snipe pop sizes across 100 runs\nbirth cost = 7.5, neg nutrition = -0.1", xlab="snipe variety", layout=c(5,1), panel=function(x,y,...){panel.hanoi(x,y,...);panel.bwplot(x, y, ...)})

Actually, rather than converting step to a factor, as above, leave it as
numeric, and then use as.factor to convert it on the fly.  This allows
you to filter on step as a numeric value.  Example:

bwplot(count ~ snipe_class | as.factor(step), data=kvsr[kvsr$step>=550,], main="k-snipe, r-snipe pop sizes across 50 runs\nbirth cost = 7.5, neg nutrition = -0.1", xlab="snipe variety")


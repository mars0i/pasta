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

# with hanoi, bar for median, red dot for mean:
panel.mean <- function(x, y, ...) {
    tmp <- tapply(y, x, FUN = mean);
    panel.points(y=tmp, x=seq_along(tmp), ...)
}
bwplot(count ~ snipe_class | as.factor(step), data=yo, layout=c(10,3), panel=function(x,y,...){panel.bwplot(x,y, pch="|",...);panel.mean(x,y,pch=".", col="red", cex=5);panel.hanoi(x,y,...)}, main="k-snipe and r-snipe counts at different timesteps across 100 runs with default parameters", scales=list(alternating=c(3,3)))


# How to pull data from the first 50 of the runs:
makeCountTimeseriesPlots(kvsr[kvsr$step>=500 & kvsr$run %in% unique(kvsr$run)[1:50],], c(10,5), "r", "Yow")

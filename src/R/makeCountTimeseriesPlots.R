# This software is copyright 2017 by Marshall Abrams, and is distributed
# under the Gnu General Public License version 3.0 as specified in the
# the file LICENSE.

# makeCountTimeseriesPlots.R
# function to make a lattice of plots of counts of abs freqs over time
# for two competing snipe types in pasta.

require(latticeExtra)

# dframe should be a dataframe with these columns:
# [1] "run"         "step"        "snipe_class" "subenv"      "pref_sign"   "count"       "energy"      "pref"        "age"        
# layoutdims should be a two-element vector specifying the layout of the panels
# example usage:
#    makeCountlTimeseriesPlots(kvsr, c(10,5), "r", "K vs R")
# Wrap this in pdf() to make a pdf
makeCountTimeseriesPlots <- function(dframe, layoutdims, altsnipes, title){
  dframe$run <- factor(dframe$run)
  dframe.ag <- aggregate(count ~ snipe_class * run * step, dframe, sum)
  altsnipes <- xyplot(count ~ step | run, data=dframe.ag[dframe.ag$snipe_class==altsnipes,],
                                  type=c("l"),  # draw lines between points
				  col="red",    # color of line
				  layout=layoutdims, # layout of panels in big plot window
				  main=title,        # main title
				  xlab="timestep\n", ylab="number of snipes", # x, y labels
				  key=list(columns=2,         # This is making the legend, with two columns
				           space="bottom",    # where it is
				           text=list(lab=c(paste(altsnipes, "-snipes", sep=""), "k-snipes")), # names of items
			                   lines=list(col=c("red", "blue"))), # colors of example lines
				  par.strip.text=list(cex=0.5)) # font size of panel header text (run numbers)

  ksnipes <- xyplot(count ~ step | run, data=dframe.ag[dframe.ag$snipe_class=="k",], type=c("l"), col="blue")

  # put two xyplots together into one:
  altsnipes + as.layer(ksnipes) # do it this way rather than other way around because the first one sets the scale, and k is lower, in general
}

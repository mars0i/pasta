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
# Example usage with default colors and line types:
#    makeCountlTimeseriesPlots(kvsr, c(10,5), "r", "K vs R")
# Grayscale example with k-snipes lines as black (0.0) and dashed (2), r-snipe line solid (1) but gray (0.5):
#    makeCountTimeseriesPlots(kvsr, c(10,5), "r", "K vs. R", colors=gray(c(0.5,0.0)), linetypes=c(1,2))
# line types: 1: solid, 2: dashed, 3: dotted (there are other options)
# Wrap this in pdf() to make a pdf
# Set stripheight to 0 to get rid of the run id panel strips above each plot
# 
# What is altsnipes?  The function always plots k-snipes.  altsnipes specifies what
# other kind of snipe to plot.

makeCountTimeseriesPlots <- function(dframe, layoutdims, altsnipes, title, colors=c("red", "blue"), linetypes=c(1,1), stripheight=1){
  # prepare the data:
  dframe$run <- factor(dframe$run)
  dframe.ag <- aggregate(count ~ snipe_class * run * step, dframe, sum)
  # make first plot layer:
  altsnipes <- xyplot(count ~ step | run, data=dframe.ag[dframe.ag$snipe_class==altsnipes,],
                                  type="l",  # draw lines between points
                                  lty=linetypes[1], # dashed, solid, etc.
                                  lwd=2,  # line width
                                  col=colors[1],    # color of line
				  layout=layoutdims, # layout of panels in big plot window
                                  cex=10, # font size I think
				  main=title,        # main title
				  xlab="timestep\n", ylab="number of snipes", # x, y labels
				  key=list(columns=2,         # legend with two columns
				           space="bottom",    # where it is
				           text=list(lab=c(paste(altsnipes, "-snipes", sep=""), "k-snipes")), # names of items
			                   lines=list(col=colors, lty=linetypes, lwd=2)), # colors, line types, widths of example lines
				  par.strip.text=list(cex=1.0, lines=stripheight)) # font size of panel header text (run numbers)
  # make second plot layer:
  ksnipes <- xyplot(count ~ step | run, data=dframe.ag[dframe.ag$snipe_class=="k",], type=c("l"), lty=linetypes[2], lwd=2, col=colors[2])

  # put two xyplots layers together into one:
  altsnipes + as.layer(ksnipes) # do it this way rather than other way around because the first one sets the scale, and k is lower, in general
}

# special version with custom y axis limits built in
hackyMakeCountTimeseriesPlots <- function(dframe, layoutdims, altsnipes, title, colors=c("red", "blue"), linetypes=c(1,1), stripheight=1){
  # prepare the data:
  dframe$run <- factor(dframe$run)
  dframe.ag <- aggregate(count ~ snipe_class * run * step, dframe, sum)
  # make first plot layer:
  altsnipes <- xyplot(count ~ step | run, data=dframe.ag[dframe.ag$snipe_class==altsnipes,],
                                  ylim=c(0,2000), # HACK
                                  type="l",  # draw lines between points
                                  lty=linetypes[1], # dashed, solid, etc.
                                  lwd=2,  # line width
                                  col=colors[1],    # color of line
				  layout=layoutdims, # layout of panels in big plot window
                                  cex=10, # font size I think
				  main=title,        # main title
				  xlab="timestep\n", ylab="number of snipes", # x, y labels
				  key=list(columns=2,         # legend with two columns
				           space="bottom",    # where it is
				           text=list(lab=c(paste(altsnipes, "-snipes", sep=""), "k-snipes")), # names of items
			                   lines=list(col=colors, lty=linetypes, lwd=2)), # colors, line types, widths of example lines
				  par.strip.text=list(cex=1.0, lines=stripheight)) # font size of panel header text (run numbers)
  # make second plot layer:
  ksnipes <- xyplot(count ~ step | run, data=dframe.ag[dframe.ag$snipe_class=="k",], type=c("l"), lty=linetypes[2], lwd=2, col=colors[2])

  # put two xyplots layers together into one:
  altsnipes + as.layer(ksnipes) # do it this way rather than other way around because the first one sets the scale, and k is lower, in general
}

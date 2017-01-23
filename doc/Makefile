BASE=perception2
all: $(BASE).pdf
ps: ../out/$(BASE).ps
pdf: ../out/$(BASE).pdf
bib: $(BASE).bbl

$(BASE).pdf: $(BASE).tex $(ADDLTEXFILES)
	(xelatex -interaction batchmode $(BASE)) || (echo ; sleep 1; echo ; sleep 1; echo ; xelatex $(BASE))

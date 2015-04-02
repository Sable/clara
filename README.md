# Clara
Clara (CompiLe-time Approximation of Runtime Analyses) is a research framework for the implementation of hybrid typestate analyses, which use static analyses to partially evaluate runtime monitors for typestate properties. The major design goal of Clara is to de-couple the code-generation for efficient runtime monitors from the static analyses that convert these monitors into faster, residual monitors. Clara is compatible to any runtime monitor that is implemented as an AspectJ aspect. Further, researchers can easily implement static analyses that will then automatically optimize any of these monitors.

Clara is built on top of the [AspectBench Compiler](http://www.sable.mcgill.ca/abc/), which uses technologies from [Soot](sable.github.io/soot/), [JastAdd](http://jastadd.org/web/), [polyglot](http://www.cs.cornell.edu/projects/polyglot/) and other open-source projects.

More information on the [Clara website](http://www.bodden.de/clara/).

package org.wiyn.odi.GuideStarContainer;

import java.util.Comparator;

public class GSC_SortByFlux implements Comparator<GuideStarContainer> {

    public int compare (GuideStarContainer o1, GuideStarContainer o2) {

	return (int) Math.signum (o1.getFlux () - o2.getFlux ());
    }

}

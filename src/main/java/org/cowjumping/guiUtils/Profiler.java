package org.cowjumping.guiUtils;


public class Profiler {

	private long start;
	private long end = 0;
	private double execTime = 0;
	private String label = null;

	public Profiler(String label) {
		this();
		this.label = label;

	}

	public Profiler() {
		this.start = System.currentTimeMillis();
	}

	public double end() {
		end = System.currentTimeMillis();
		execTime = (end - start) / 1000.;
		return execTime;
	}

	public String toString() {
		if (end == 0)
			end();
		return String.format("Profile [%s]: %8.3f s", label, execTime);
	}

}

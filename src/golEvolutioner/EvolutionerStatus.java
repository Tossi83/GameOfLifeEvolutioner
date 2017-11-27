package golEvolutioner;

public class EvolutionerStatus
	{
	public boolean
		isShuttingDown = false;
	public int
		step = 0,
		evaluationThreadsCount = 8,
		testSwitch = 19,
		evaluations = 0,
		generation = 1,
		size = 32;
	public String title = "\"Game of Life evolutioner v0.3\" by Tossi";
	}

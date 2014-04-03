package eu.compassresearch.core.interpreter.utility;

public class Pair<F, S>
{
	public final F first; // first member of pair
	public final S second; // second member of pair

	public Pair(F first, S second)
	{
		this.first = first;
		this.second = second;
	}

	@Override
	public String toString()
	{

		return "(" + this.first + "," + this.second + ")";
	}

}

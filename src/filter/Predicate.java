package filter;

import model.TurboIssue;
import model.TurboLabel;

public class Predicate implements FilterExpression {
	private String name;
	private String content;

	public Predicate(String name, String content) {
		this.name = name;
		this.content = content;
	}

	@Override
	public String toString() {
		return name + "(" + content + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Predicate other = (Predicate) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	public boolean isSatisfiedBy(TurboIssue issue) {
		switch (name) {
		case "title":
			return issue.getTitle().toLowerCase().contains(content.toLowerCase());
		case "milestone":
			return issue.getMilestone().getTitle().toLowerCase().contains(content.toLowerCase());
		case "parent":
			content = content.toLowerCase();
			if (content.startsWith("#")) {
				return issue.getParents().contains(Integer.parseInt(content.substring(1)));
			} else if (Character.isDigit(content.charAt(0))) {
				return issue.getParents().contains(Integer.parseInt(content));
			} else {
				// search parent name instead
				return false;
			}
//		case "child":
		case "label":
			for (TurboLabel l : issue.getLabels()) {
				if (l.getName().toLowerCase().contains(content.toLowerCase())) {
					return true;
				}
			}
			return false;
		default:
			return false;
		}
	}
}
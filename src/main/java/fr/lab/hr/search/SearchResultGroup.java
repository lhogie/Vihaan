package fr.lab.hr.search;

import java.util.List;

public record SearchResultGroup(String label, String urlPrefix, List<SearchHit> hits) {
}

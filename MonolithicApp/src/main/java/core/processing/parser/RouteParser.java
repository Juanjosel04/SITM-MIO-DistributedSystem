package core.processing.parser;

import core.model.Route;
import core.utils.CsvUtils;
import shared.exceptions.CsvParsingException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteParser {
    public Route parse(String line, List<String> header) throws CsvParsingException {
        List<String> values = CsvUtils.split(line);
        Map<String, Integer> index = buildIndex(header);

        try {
            int id = Integer.parseInt(getValue(values, index, "LINEID", 0));
            String shortName = getValue(values, index, "SHORTNAME", 2);
            String description = getValue(values, index, "DESCRIPTION", 3);
            return new Route(id, shortName, description, "ACTIVE");
        } catch (RuntimeException exception) {
            throw new CsvParsingException("Invalid route line", exception);
        }
    }

    private Map<String, Integer> buildIndex(List<String> header) {
        Map<String, Integer> index = new HashMap<String, Integer>();
        for (int i = 0; i < header.size(); i++) {
            String key = CsvUtils.clean(header.get(i)).toUpperCase();
            if (!index.containsKey(key)) {
                index.put(key, i);
            }
        }
        return index;
    }

    private String getValue(List<String> values, Map<String, Integer> index, String name, int fallbackIndex) {
        Integer resolvedIndex = index.get(name);
        int position = resolvedIndex == null ? fallbackIndex : resolvedIndex.intValue();
        if (position < 0 || position >= values.size()) {
            return "";
        }
        return CsvUtils.clean(values.get(position));
    }
}

package core.ingestion;

import core.model.Route;
import core.processing.parser.RouteParser;
import persistence.repository.RouteRepository;
import shared.exceptions.CsvParsingException;
import shared.exceptions.RepositoryException;

import java.io.IOException;

public class RouteLoader {
    private final CsvReader csvReader;
    private final RouteParser routeParser;
    private final RouteRepository routeRepository;

    public RouteLoader(CsvReader csvReader, RouteParser routeParser, RouteRepository routeRepository) {
        this.csvReader = csvReader;
        this.routeParser = routeParser;
        this.routeRepository = routeRepository;
    }

    public int loadRoutes(String path, boolean persistenceEnabled) throws IOException {
        CsvContent content = csvReader.read(path, true);
        int loaded = 0;
        for (String line : content.getLines()) {
            try {
                Route route = routeParser.parse(line, content.getHeader());
                if (persistenceEnabled) {
                    routeRepository.save(route);
                }
                loaded++;
            } catch (CsvParsingException ignored) {
            } catch (RepositoryException exception) {
                throw new IOException("Could not persist routes", exception);
            }
        }
        return loaded;
    }
}

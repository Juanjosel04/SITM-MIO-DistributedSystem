package core.ingestion;

import core.model.Route;
import core.processing.parser.RouteParser;
import persistence.repository.RouteRepository;
import shared.exceptions.CsvParsingException;
import shared.exceptions.RepositoryException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        return loadRouteCatalog(path, persistenceEnabled).size();
    }

    public List<Route> loadRouteCatalog(String path, boolean persistenceEnabled) throws IOException {
        CsvContent content = csvReader.read(path, true);
        List<Route> routes = new ArrayList<Route>();
        for (String line : content.getLines()) {
            try {
                Route route = routeParser.parse(line, content.getHeader());
                if (persistenceEnabled) {
                    routeRepository.save(route);
                }
                routes.add(route);
            } catch (CsvParsingException ignored) {
            } catch (RepositoryException exception) {
                throw new IOException("Could not persist routes", exception);
            }
        }
        return routes;
    }
}

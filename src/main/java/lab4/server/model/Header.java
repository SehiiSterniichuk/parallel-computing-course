package lab4.server.model;

import lab4.config.RequestType;
import lab4.server.model.header.HeaderParametersHolder;
public record Header(RequestType type, HeaderParametersHolder parameters) {
}

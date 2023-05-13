package lab4.model;

import lab4.model.header.HeaderParametersHolder;
public record Header(RequestType type, HeaderParametersHolder parameters) {
}

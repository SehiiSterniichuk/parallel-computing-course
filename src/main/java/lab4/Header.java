package lab4;

import lab4.header.parameters.HeaderParametersHolder;
public record Header(RequestType type, HeaderParametersHolder parameters) {
}

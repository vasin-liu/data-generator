package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;

public class TemplateV2RuntimeRegistryBuildException extends RuntimeException {
    public TemplateV2RuntimeRegistryBuildException(String message, Throwable cause) {
        super(message, cause);
    }
}

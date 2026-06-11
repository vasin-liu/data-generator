package org.gensokyo.data.calcite.plugin;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.runtime.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;

public class TemplateV2RuntimePluginContractException extends RuntimeException {
    public TemplateV2RuntimePluginContractException(String message) {
        super(message);
    }
}

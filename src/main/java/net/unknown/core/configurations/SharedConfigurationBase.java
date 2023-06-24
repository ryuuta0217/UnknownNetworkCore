package net.unknown.core.configurations;

import net.unknown.shared.SharedConstants;

import java.io.File;

public abstract class SharedConfigurationBase extends ConfigurationBase {
    public SharedConfigurationBase(String fileName, String loggerName) {
        this(fileName, loggerName, '.');
    }

    public SharedConfigurationBase(String fileName, String loggerName, char configurationPathSeparator) {
        this(new File(SharedConstants.DATA_FOLDER, fileName), loggerName, configurationPathSeparator);
    }

    public SharedConfigurationBase(File file, String loggerName, char configurationPathSeparator) {
        super(file, false, loggerName, configurationPathSeparator);
    }
}

package org.jd.core.v1.printer;

import org.jd.core.v1.api.Decompiler;
import org.jd.core.v1.api.loader.Loader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.bcel.Const.MAJOR_1_1;
import static org.apache.bcel.Const.MAJOR_1_5;
import static jd.core.preferences.Preferences.ESCAPE_UNICODE_CHARACTERS;
import static jd.core.preferences.Preferences.REALIGN_LINE_NUMBERS;
import static jd.core.preferences.Preferences.WRITE_LINE_NUMBERS;
import static jd.core.preferences.Preferences.WRITE_METADATA;

import jd.core.ClassUtil;
import jd.core.DecompilationResult;
import jd.core.links.DeclarationData;
import jd.core.links.HyperlinkData;
import jd.core.links.HyperlinkReferenceData;
import jd.core.links.ReferenceData;
import jd.core.links.StringData;

public class ClassFilePrinter extends LineNumberStringBuilderPrinter {

    private final Map<String, ReferenceData> referencesCache = new HashMap<>();
    private final DecompilationResult result = new DecompilationResult();

    // Manage line number and misalignment
    private int textAreaLineNumber = 1;

    @Override
    public void start(int maxLineNumber, int majorVersion, int minorVersion) {
        super.start(maxLineNumber, majorVersion, minorVersion);

        if (maxLineNumber != 0) {
            result.setMaxLineNumber(maxLineNumber);
        }
    }

    // --- Add strings --- //
    @Override
    public void printStringConstant(String constant, String ownerInternalName) {
        if (constant == null) {
            constant = "null";
        }
        if (ownerInternalName == null) {
            ownerInternalName = "null";
        }

        result.addString(new StringData(stringBuffer.length(), constant, ownerInternalName));
        super.printStringConstant(constant, ownerInternalName);
    }

    @Override
    public void printDeclaration(int type, String internalTypeName, String name, String descriptor) {
        if (internalTypeName == null) {
            internalTypeName = "null";
        }
        if (name == null) {
            name = "null";
        }
        if (descriptor == null) {
            descriptor = "null";
        }

        switch (type) {
        case TYPE:
            DeclarationData data = new DeclarationData(stringBuffer.length(), name.length(), internalTypeName, null,
                    null);
            result.addDeclaration(internalTypeName, data);
            result.addTypeDeclaration(stringBuffer.length(), data);
            break;
        case CONSTRUCTOR:
            result.addDeclaration(internalTypeName + "-<init>-" + descriptor, new DeclarationData(stringBuffer.length(), name.length(), internalTypeName, "<init>", descriptor));
            break;
        default:
            result.addDeclaration(internalTypeName + '-' + name + '-' + descriptor, new DeclarationData(stringBuffer.length(), name.length(), internalTypeName, name, descriptor));
            break;
        }
        super.printDeclaration(type, internalTypeName, name, descriptor);
    }

    @Override
    public void printReference(int type, String internalTypeName, String name, String descriptor,
            String ownerInternalName) {
        if (internalTypeName == null) {
            internalTypeName = "null";
        }
        if (name == null) {
            name = "null";
        }
        if (descriptor == null) {
            descriptor = "null";
        }

        switch (type) {
            case TYPE:
                addHyperlink(new HyperlinkReferenceData(stringBuffer.length(), name.length(), newReferenceData(internalTypeName, null, null, ownerInternalName)));
                break;
            case CONSTRUCTOR:
                addHyperlink(new HyperlinkReferenceData(stringBuffer.length(), name.length(), newReferenceData(internalTypeName, "<init>", descriptor, ownerInternalName)));
                break;
            default:
                addHyperlink(new HyperlinkReferenceData(stringBuffer.length(), name.length(), newReferenceData(internalTypeName, name, descriptor, ownerInternalName)));
                break;
        }
        super.printReference(type, internalTypeName, name, descriptor, ownerInternalName);
    }

    public void addHyperlink(HyperlinkData hyperlinkData) {
        result.addHyperLink(hyperlinkData.getStartPosition(), hyperlinkData);
    }

    @Override
    public void startLine(int lineNumber) {
        super.startLine(lineNumber);
        result.putLineNumber(textAreaLineNumber, lineNumber);
    }

    @Override
    public void endLine() {
        super.endLine();
        textAreaLineNumber++;
    }

    @Override
    public void extraLine(int count) {
        super.extraLine(count);
        if (realignmentLineNumber) {
            textAreaLineNumber += count;
        }
    }

    // --- Add references --- //
    public ReferenceData newReferenceData(String internalName, String name, String descriptor, String scopeInternalName) {
        String key = internalName + '-' + name + '-'+ descriptor + '-' + scopeInternalName;
        return referencesCache.computeIfAbsent(key, k -> {
            ReferenceData reference = new ReferenceData(internalName, name, descriptor, scopeInternalName);
            result.addReference(reference);
            return reference;
        });
    }

    public DecompilationResult getResult() {
        return result;
    }

    public String buildDecompiledOutput(Map<String, String> preferences, Loader loader, String entryPath, Decompiler decompiler) throws IOException {
        // Init preferences
        boolean realignmentLineNumbers = Boolean.parseBoolean(preferences.getOrDefault(REALIGN_LINE_NUMBERS, Boolean.FALSE.toString()));

        Map<String, Object> configuration = new HashMap<>();
        configuration.put("realignLineNumbers", realignmentLineNumbers);

        setRealignmentLineNumber(realignmentLineNumbers);
        setUnicodeEscape(Boolean.parseBoolean(preferences.getOrDefault(ESCAPE_UNICODE_CHARACTERS, Boolean.FALSE.toString())));
        setShowLineNumbers(Boolean.parseBoolean(preferences.getOrDefault(WRITE_LINE_NUMBERS, Boolean.TRUE.toString())));

        // Format internal name
        String entryInternalName = ClassUtil.getInternalName(entryPath);

        // Decompile class file
        decompiler.decompile(loader, this, entryInternalName, configuration);

        StringBuilder stringBuffer = getStringBuffer();

        // Metadata
        if (Boolean.parseBoolean(preferences.getOrDefault(WRITE_METADATA, Boolean.TRUE.toString()))) {
            stringBuffer.append("\n\n/*");
            // Add Java compiler version
            int majorVersion = getMajorVersion();

            if (majorVersion >= MAJOR_1_1) {
                stringBuffer.append("\n * Java compiler version: ");

                if (majorVersion >= MAJOR_1_5) {
                    stringBuffer.append(majorVersion - (MAJOR_1_5 - 5));
                } else {
                    stringBuffer.append("1.");
                    stringBuffer.append(majorVersion - (MAJOR_1_1 - 1));
                }

                stringBuffer.append(" (");
                stringBuffer.append(majorVersion);
                stringBuffer.append('.');
                stringBuffer.append(getMinorVersion());
                stringBuffer.append(')');
            }
            // Add JD-Core version
            stringBuffer.append("\n * JD-Core Version:       ");
            stringBuffer.append(getVersion());
            stringBuffer.append("\n */");
        }
        return stringBuffer.toString();
    }
}

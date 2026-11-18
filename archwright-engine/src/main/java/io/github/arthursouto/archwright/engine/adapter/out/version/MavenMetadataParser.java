package io.github.arthursouto.archwright.engine.adapter.out.version;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.IntStream;

final class MavenMetadataParser {

    private static final String VERSIONS = "versions";
    private static final String VERSION = "version";

    List<String> versions(String xml) throws IOException {
        try {
            Document document = builder().parse(new InputSource(new StringReader(xml)));

            return versionTexts(versionsContainer(document));
        } catch (ParserConfigurationException | SAXException exception) {
            throw new IOException("invalid maven-metadata.xml: " + exception.getMessage(), exception);
        }
    }

    private DocumentBuilder builder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        return factory.newDocumentBuilder();
    }

    private Node versionsContainer(Document document) throws IOException {
        NodeList containers = document.getElementsByTagName(VERSIONS);

        if (containers.getLength() == 0) {
            throw new IOException("invalid maven-metadata.xml: no <versions> element");
        }

        return containers.item(0);
    }

    private List<String> versionTexts(Node container) {
        NodeList children = container.getChildNodes();

        return IntStream.range(0, children.getLength())
                .mapToObj(children::item)
                .filter(node -> VERSION.equals(node.getNodeName()))
                .map(node -> node.getTextContent().trim())
                .filter(text -> !text.isEmpty())
                .toList();
    }
}

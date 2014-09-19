Presents the file stream parser that de/encoding "B-encode" data format. That
used by the peer-to-peer file sharing system (like BitTorrent) for storing
and transmitting loosely structured data.

BeeCoder serialize the ByteArrayInputStream that contains raw data, of supported java objects
And deserialize the ObjectInputStream that contains Bencoded types of java objects

Supported types:
<ul>
<li>java.lang.Integer for integers</li>
<li>java.lang.String for strings</li>
<li>java.util.LinkedList<Object> for lists</li>
<li>java.util.LinkedList<Entry<String, Object>> for dictionaries</li>
</ul>
So client application doesn't needs to import some additional types.

For dictionary realization was chosen LinkedList<Entry<String, Object>> instead of LinkedHashMap please see benchmark page
<a href="https://github.com/soulaway/jse8collectionBenchmark">JSE8 collections insert/iterate benchmark</a>

Author Dmitry G. Soloviev

More about <a href="http://en.wikipedia.org/wiki/Bencode">bencode</a>. 
Sources <a href="https://github.com/soulaway/beecoder">here</a>

package moonkev.zmq.spring.integration.convert;

import java.io.IOException;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;

public class MapToJsonByteArrayConverter implements Converter<Map<Object, Object>, byte[]> {

	private static final ObjectMapper mapper = new ObjectMapper();
	
	public byte[] convert(Map<Object, Object> map) {
		try {
			return mapper.writeValueAsBytes(map);
		} catch (IOException e) {
			throw new ConversionFailedException(
					TypeDescriptor.valueOf(Map.class), 
					TypeDescriptor.valueOf(byte[].class), 
					map, e);
		}
	}
}

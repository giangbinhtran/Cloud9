/*
 * Cloud9: A MapReduce Library for Hadoop
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package edu.umd.cloud9.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import junit.framework.JUnit4TestAdapter;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.junit.Test;

public class VectorIntTest {

	@Test
	public void testBasic() throws IOException {
		VectorInt<Text> map = new VectorInt<Text>();

		map.set(new Text("hi"), 5);
		map.set(new Text("there"), 22);

		Text key;
		int value;

		assertEquals(map.size(), 2);

		key = new Text("hi");
		value = map.get(key);
		assertEquals(value, 5);

		value = map.remove(key);
		assertEquals(map.size(), 1);

		key = new Text("there");
		value = map.get(key);
		assertEquals(value, 22);
	}

	@Test
	public void testSerialize1() throws IOException {
		VectorInt<Text> origMap = new VectorInt<Text>();

		origMap.set(new Text("hi"), 5);
		origMap.set(new Text("there"), 22);

		ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
		DataOutputStream dataOut = new DataOutputStream(bytesOut);

		origMap.write(dataOut);

		VectorInt<Text> map = new VectorInt<Text>();

		map.readFields(new DataInputStream(new ByteArrayInputStream(bytesOut
				.toByteArray())));

		Text key;
		int value;

		assertEquals(map.size(), 2);

		key = new Text("hi");
		value = map.get(key);
		assertEquals(value, 5);

		value = map.remove(key);
		assertEquals(map.size(), 1);

		key = new Text("there");
		value = map.get(key);
		assertEquals(value, 22);
	}

	@Test(expected = IOException.class)
	public void testTypeSafety() throws IOException {
		VectorInt<WritableComparable> origMap = new VectorInt<WritableComparable>();

		origMap.set(new Text("hi"), 4);
		origMap.set(new IntWritable(0), 76);

		ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
		DataOutputStream dataOut = new DataOutputStream(bytesOut);

		origMap.write(dataOut);

		VectorInt<WritableComparable> map = new VectorInt<WritableComparable>();

		map.readFields(new DataInputStream(new ByteArrayInputStream(bytesOut
				.toByteArray())));

	}

	@Test
	public void testSerializeEmpty() throws IOException {
		VectorInt<WritableComparable> map = new VectorInt<WritableComparable>();

		assertTrue(map.size() == 0);

		ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
		DataOutputStream dataOut = new DataOutputStream(bytesOut);

		map.write(dataOut);

		VectorInt<WritableComparable> newList = new VectorInt<WritableComparable>();
		newList.readFields(new DataInputStream(new ByteArrayInputStream(
				bytesOut.toByteArray())));
		assertTrue(newList.size() == 0);
	}

	@Test
	public void testPlus() throws IOException {
		VectorInt<Text> map1 = new VectorInt<Text>();

		map1.set(new Text("hi"), 5);
		map1.set(new Text("there"), 22);

		VectorInt<Text> map2 = new VectorInt<Text>();

		map2.set(new Text("hi"), 4);
		map2.set(new Text("test"), 5);

		map1.plus(map2);

		assertEquals(map1.size(), 3);
		assertTrue(map1.get(new Text("hi")) == 9);
		assertTrue(map1.get(new Text("there")) == 22);
		assertTrue(map1.get(new Text("test")) == 5);
	}

	@Test
	public void testDot() throws IOException {
		VectorInt<Text> map1 = new VectorInt<Text>();

		map1.set(new Text("hi"), 5);
		map1.set(new Text("there"), 2);
		map1.set(new Text("empty"), 3);

		VectorInt<Text> map2 = new VectorInt<Text>();

		map2.set(new Text("hi"), 4);
		map2.set(new Text("there"), 4);
		map2.set(new Text("test"), 5);

		int s = map1.dot(map2);

		assertEquals(s, 28);
	}
	
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(VectorIntTest.class);
	}

}

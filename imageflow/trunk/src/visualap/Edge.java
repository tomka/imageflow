/*
Version 1.0, 30-12-2007, First release

IMPORTANT NOTICE, please read:

This software is licensed under the terms of the GNU GENERAL PUBLIC LICENSE,
please read the enclosed file license.txt or http://www.gnu.org/licenses/licenses.html

Note that this software is freeware and it is not designed, licensed or intended
for use in mission critical, life support and military purposes.

The use of this software is at the risk of the user.
*/

/* Edge.java

javalc6
*/
package visualap;

public class Edge {
	transient public Pin from, to;
	
	public Edge(Pin from, Pin to) {
		this.from = from;
		this.to = to;
	}

	public String toString() {
		return from.getName()+","+to.getName();
	}
}
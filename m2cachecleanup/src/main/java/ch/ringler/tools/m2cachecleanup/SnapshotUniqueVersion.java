/**
 * 
 */
package ch.ringler.tools.m2cachecleanup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Helper class holding Timestamp and Build number. Class is comparable to
 * itself
 * 
 */
public final class SnapshotUniqueVersion implements
		Comparable<SnapshotUniqueVersion> {
	private final Date m_timestamp;
	private final int m_buildNo;

	SnapshotUniqueVersion(String date, String time, String buildNo)
			throws ParseException {
		m_timestamp = getFormat().parse(date + "." + time);
		m_buildNo = Integer.parseInt(buildNo);
	}

	public Date getTimestamp() {
		return m_timestamp;
	}

	public int getBuildNo() {
		return m_buildNo;
	}

	@Override
	public String toString() {
		return getFormat().format(m_timestamp) + "-"
				+ Integer.toString(m_buildNo);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (obj instanceof SnapshotUniqueVersion) {
			SnapshotUniqueVersion ver = (SnapshotUniqueVersion) obj;
			return (getTimestamp().getTime() == ver.getTimestamp().getTime())
					&& (getBuildNo() == ver.getBuildNo());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	public int compareTo(SnapshotUniqueVersion o) {
		if (o == null)
			return 1; // Non-null is greater then null

		int retval = getTimestamp().compareTo(o.getTimestamp());
		if (retval != 0)
			return retval;

		// Compare build Numbers
		if (getBuildNo() < o.getBuildNo())
			return -1;
		if (getBuildNo() > o.getBuildNo())
			return 1;
		return 0;
	}

	private static SimpleDateFormat getFormat() {
		SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd.HHmmss");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		return fmt;
	}
}

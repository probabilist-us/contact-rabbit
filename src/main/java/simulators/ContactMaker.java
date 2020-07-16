package simulators;

import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Set;
/**
 * 
 * @author rwdarli
 *
 * @param <M> is the type of the mobileIDs
 */
public interface ContactMaker<M> {
	LongSummaryStatistics exposureCountSummary();
	Map<Long, Long> tallyExposureStatistics();
	Set<M> getExposedMobileIDs();
	Set<M> getInfectedMobileIDs();
	
}

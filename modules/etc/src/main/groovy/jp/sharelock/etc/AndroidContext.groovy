package jp.sharelock.etc

import android.content.Context

@groovy.transform.CompileStatic
/**
 *
 * @author Alberto Lepe <lepe@sharelock.jp>
 */
class AndroidContext {
	public static Context context
	static InputStream openAsset(String filename) throws IOException, NullPointerException {
		if(context != null) {
			return context.getAssets().open(filename)
		} else {
			throw new NullPointerException()
		}
	}
}

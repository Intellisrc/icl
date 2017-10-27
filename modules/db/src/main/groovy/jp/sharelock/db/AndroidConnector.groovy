package jp.sharelock.db

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

import jp.sharelock.db.DB.Connector
import jp.sharelock.db.DB.ColumnType
import jp.sharelock.db.DB.DBType
import jp.sharelock.db.DB.Statement
import jp.sharelock.etc.Config
import jp.sharelock.etc.Log

@groovy.transform.CompileStatic
/**
 * @author Alberto Lepe <lepe@sharelock.jp>
 */
class AndroidConnector implements Connector {
	private final String LOG_TAG = AndroidConnector.getSimpleName()
    private Updater dbhelper
    private SQLiteDatabase db
	private String dbname
	private Context context
	private int version
	long lastUsed = 0
	
    /////////////////////////// Constructors /////////////////////////////
	AndroidConnector(Object context) {
		this("", (Context) context)
	}
	AndroidConnector(Context context) {
		this("", context)
	}
    AndroidConnector(String dbname, Object context) {
        this(dbname, (Context) context)
    }
    AndroidConnector(String dbname, Context context, int version = 1) {
		if(!dbname) {
            if(Config.hasKey("db.name")) {
                this.dbname = Config.get("db.name") + ".db"
            } else {
                Log.e(LOG_TAG, "Database is not defined. Please pass it to the constructor or define 'db.name' in config.properties")
            }
		} else {
			this.dbname = dbname + ".db"
		}
		this.context = context
		this.version = version
    }

    /**
     * Returns database version
	 * @return version
     */
    int version() {
        return db?.getVersion()
    }	

	/**      
	 * Set interface when version is updated / downgrated      
	 * @param updater      
	 */     
	void onUpdate(DBUpdate updater) {
		dbhelper?.setUpdater(updater)
	}

	@Override
	boolean isOpen() {
		return db?.isOpen()
	}

	@Override
	void open() {
		if(dbhelper == null) {
			dbhelper = new Updater(dbname, context, version)
		}
        try {
            db = dbhelper.getWritableDatabase()
        } catch(e1) {
            Log.w(LOG_TAG, "Unable to get Writable Database, trying with read-only : "+e1)
            try {
                db = dbhelper.getReadableDatabase()
            } catch (e2) {
                Log.e(LOG_TAG, "Unable to open Database : "+e2)
            }
        }
	}

	@Override
	void close() {
		db?.close()
	}

	@Override
	Statement prepare(Query query) {
		final Cursor cursor = db?.rawQuery(query.toString(), query.getArgsStr())
		return new Statement() {
			@Override
			boolean next() {
				return cursor?.moveToNext()
			}

			@Override
			void close() {
				cursor?.close()
			}

			@Override
			int columnCount() {
				return cursor?.getColumnCount()
			}

			@Override
			ColumnType columnType(int index) {
				ColumnType ct = ColumnType.NULL
				switch(cursor?.getType(index)) {
					case Cursor.FIELD_TYPE_STRING: ct = ColumnType.TEXT; break
					case Cursor.FIELD_TYPE_INTEGER: ct = ColumnType.INTEGER; break
					case Cursor.FIELD_TYPE_FLOAT: ct = ColumnType.DOUBLE; break
					case Cursor.FIELD_TYPE_BLOB: ct = ColumnType.BLOB; break
				}
				return ct
			}

			@Override
			String columnName(int index) {
				return cursor?.getColumnName(index)
			}

			@Override
			String columnStr(int index) {
				return cursor?.getString(index)
			}

			@Override
			Integer columnInt(int index) {
				return cursor?.getInt(index)
			}

			@Override
			Double columnDbl(int index) {
				return cursor?.getDouble(index)
			}

			@Override
			byte[] columnBlob(int index) {
				return cursor?.getBlob(index)
			}

			@Override
			Date columnDate(int index) {
				try {
					return new Date()
				} catch (Exception ex) {
					Log.e(LOG_TAG, "column Date failed: "+ex)
					return null
				}
			}

			@Override
			boolean isColumnNull(int index) {
				return cursor?.isNull(index)
			}

			@Override
			int firstColumn() {
				return 0
			}
		}
	}

	@Override
	void onError(Exception ex) {
		Log.e(LOG_TAG, ex)
	}

	@Override
	DBType getType() {
		return DBType.SQLITE
	}
	/**
	 * Interface to execute code on Upgrade / Downgrade
	 * "oldversion" is the database current version (before updating)
	 * it can be used to perform specific changes, e.g:
	 * <code>
		 // new version will be : 24
		 switch(oldversion) {
			case 21 : db.set("ALTER TABLE ADD COLUMN x ...")
			case 22 : db.set("ALTER TABLE ADD COLUMN y ...")
			case 23 : db.set("ALTER TABLE DROP COLUMN x ...")
		 }
	 * </code>
	 */
	interface DBUpdate {
		void onUpgrade(SQLiteDatabase db, int oldversion)
		void onDowngrade(SQLiteDatabase db, int oldversion)
	}
	/**
	 * Subclass taking care of Database updates
	 */
	class Updater extends SQLiteOpenHelper {
		private DBUpdate updater

		Updater(String dbname, Context context, int version) {
			super(context, dbname, null, version)
		}
		@Override
		void onCreate(SQLiteDatabase db) {
			Log.w(LOG_TAG,"Database created")
		}

		@Override
		void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(LOG_TAG,"Newer Database detected")
			if(this.updater != null) {
				updater?.onUpgrade(db, oldVersion)
			}
		}
		@Override
		void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(LOG_TAG,"Older Database detected")
			if(this.updater != null) {
				updater?.onDowngrade(db, oldVersion)
			}
		}

		void setUpdater(DBUpdate updater) {
			this.updater = updater
		}

	}
}

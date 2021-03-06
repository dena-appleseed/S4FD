package org.mightyfrog.android.s4fd.main

import android.Manifest
import android.app.Activity
import android.content.Context
import android.util.Log
import com.raizlabs.android.dbflow.config.FlowManager
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.raizlabs.android.dbflow.sql.language.Select
import com.tbruyelle.rxpermissions.RxPermissions
import org.mightyfrog.android.s4fd.R
import org.mightyfrog.android.s4fd.data.*
import org.mightyfrog.android.s4fd.util.Const
import org.mightyfrog.android.s4fd.util.KHService
import rx.Observable
import rx.SingleSubscriber
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import java.io.File
import java.io.InputStream
import javax.inject.Inject

/**
 * TODO: rewrite data retrieval code, needs major refactoring
 * @author Shigehiro Soejima
 */
class MainPresenter @Inject constructor(val mView: MainContract.View, val mKHService: KHService) : MainContract.Presenter {
    private val mCompositeSubscription: CompositeSubscription = CompositeSubscription()

    init {
        mView.setPresenter(this)
    }

    override fun loadCharacters() {
        val characterList = Select().from(KHCharacter::class.java)
                .where()
                .queryList()
        if (characterList.size != Const.CHARACTER_COUNT) {
            mCompositeSubscription.add(mKHService.getCharacters()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : SingleSubscriber<List<KHCharacter>>() {
                        override fun onSuccess(list: List<KHCharacter>?) {
                            if (list == null) {
                                mView.showErrorMessage((mView as Context).getString(R.string.no_char_data_found))
                                return
                            }
                            FlowManager.getDatabase(AppDatabase::class.java).executeTransaction {
                                for (char in list) {
                                    char.save()
                                }
                            }
                            loadDetails(list)
                        }

                        override fun onError(t: Throwable?) {
                            mView.showFallbackDialog()
                            Log.e(Const.TAG, t.toString())
                        }
                    }))
        } else {
            loadDetails(characterList)
        }
    }

    override fun openCharacter(id: Int, position: Int) {
        mView.showDetails(id, position)
    }

    override fun fallback() {
        RxPermissions((mView as Activity))
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe({
                    if (it) {
                        copyDatabase()
                    } else {
                        mView.finish()
                    }
                })
    }

    override fun destroy() {
        if (!mCompositeSubscription.isUnsubscribed) {
            mCompositeSubscription.unsubscribe()
        }
    }

    private fun copyDatabase() {
        (mView as Activity).assets.open("smash4data.db").use { input ->
            input.toFile(mView.getDatabasePath(AppDatabase.NAME + ".db"))
        }
        mView.showDatabaseCopiedDialog()
    }

    fun InputStream.toFile(file: File) {
        use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
    }

    private fun loadSmashAttributeTypes() { // TODO: rewrite me
        val test = SQLite.select()
                .from(SmashAttributeType::class.java)
                .where(Move_Table.id.eq(1))
                .querySingle()
        test?.let {
            return
        }

        mView.showProgressDialog((mView as Activity).getString(R.string.loading_attr_types))
        mCompositeSubscription.add(mKHService.getSmashAttributeTypes()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleSubscriber<List<SmashAttributeType>>() {
                    override fun onSuccess(attrTypes: List<SmashAttributeType>?) {
                        attrTypes?.let {
                            for (attrType in attrTypes) {
                                attrType.save()
                            }
                        }
                        loadMoves()
                    }

                    override fun onError(error: Throwable?) {
                        mView.hideActivityCircle()
                    }
                })
        )
    }

    private fun loadMoves() { // TODO: rewrite me
        val test = SQLite.select()
                .from(Move::class.java)
                .where(Move_Table.id.eq(1))
                .querySingle()
        test?.let {
            return
        }

        mView.showProgressDialog((mView as Activity).getString(R.string.loading_moves))
        mCompositeSubscription.add(mKHService.getMoves()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleSubscriber<List<Move>>() {
                    override fun onError(error: Throwable?) {
                        mView.hideProgressDialog()
                    }

                    override fun onSuccess(moves: List<Move>?) {
                        mView.hideProgressDialog()
                        moves?.let {
                            FlowManager.getDatabase(AppDatabase::class.java).executeTransaction {
                                for (move in moves) {
                                    move.save()
                                }
                            }
                        }
                    }
                }))
    }

    private fun loadDetails(list: List<KHCharacter>?) { // TODO: rewrite me
        mCompositeSubscription.add(Observable.from(list)
                .filter({ character ->
                    val metadata = SQLite.select()
                            .from(Metadata::class.java)
                            .where(Metadata_Table.id.eq(character!!.id))
                            .querySingle()
                    metadata == null
                })
                .concatMap({ character ->
                    val res = mKHService.getDetails(character.id).execute()
                    if (res.isSuccessful && res.code() == 200) {
                        val details = res.body()
                        if (details != null) {
                            FlowManager.getDatabase(AppDatabase::class.java).executeTransaction {
                                details.metadata!!.save()
                                for (move in details.movementData!!) {
                                    move.save()
                                }
                                for (attr in details.characterAttributeData!!) {
                                    attr.save()
                                }
                            }
                        }
                        Observable.just(details)
                    } else {
                        Observable.error(RuntimeException("Unable to retrieve data"))
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Subscriber<CharacterDetails>() {
                    override fun onCompleted() {
                        mView.showCharacters(SQLite.select()
                                .from(KHCharacter::class.java)
                                .queryList())
                        loadSmashAttributeTypes()
                    }

                    override fun onError(e: Throwable?) {
                        mView.showErrorMessage(e.toString())
                        mView.hideProgressDialog()
                    }

                    override fun onNext(t: CharacterDetails?) {
                        mView.showProgressDialog((mView as Activity).getString(R.string.loading_chars, t!!.metadata!!.displayName))
                    }
                })
        )
    }
}
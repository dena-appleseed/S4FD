package org.mightyfrog.android.s4fd.comparemiscs

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import org.mightyfrog.android.s4fd.R
import org.mightyfrog.widget.CenteringRecyclerView

/**
 * @author Shigehiro Soejima
 */
class CompareMiscsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ownerId = intent.getIntExtra("ownerId", 0)
        setTheme(resources.getIdentifier("CharTheme." + ownerId, "style", packageName))

        setContentView(R.layout.activity_compare_miscs)

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        initActionBar()

        val rv = findViewById(R.id.recyclerView) as CenteringRecyclerView
        rv.layoutManager = GridLayoutManager(this, 1)
        rv.adapter = DataAdapter(intent.getStringExtra("name"), ownerId, intent.getIntExtra("charToCompareId", 0))
        rv.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_out_down)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initActionBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val name = intent.getStringExtra("name")
        when (name) {
            "SPOTDODGE" -> supportActionBar?.title = "Spot Dodge"
            "AIRDODGE" -> supportActionBar?.title = "Air Dodge"
            "ROLLS" -> supportActionBar?.title = "Forward/Back Roll"
            "LEDGEROLL" -> supportActionBar?.title = "Ledge Roll"
        }
    }
}
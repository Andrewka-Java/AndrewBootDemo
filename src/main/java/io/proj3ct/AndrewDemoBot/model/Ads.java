/*
 *   Developed by Andrei Muryn© 2022
 */

package io.proj3ct.AndrewDemoBot.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity(name = "ads")
public class Ads {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String ad;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getAd() {
        return ad;
    }

    public void setAd(final String ad) {
        this.ad = ad;
    }

}

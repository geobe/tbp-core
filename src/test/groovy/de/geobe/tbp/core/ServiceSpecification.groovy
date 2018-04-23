/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018.  Georg Beier. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.geobe.tbp.core

import de.geobe.tbp.core.service.MilestoneService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

/**
 * Created by georg beier on 20.04.2018.
 */
@ContextConfiguration   // needed by Spock for Spring integration tests
@SpringBootTest         // use Spring test framework
class ServiceSpecification extends Specification {

    /** Spring beans are accessible */
    @Autowired
    private MilestoneService milestoneService

    /**
     * A Spock specification should have a long meaningful name
     */
    def "getMilestoneList should give all milestone dtos"() {
        when: "a list of milestone dtos is fetched"
        def miles = milestoneService.milestoneList
        then: 'all 4 milestones should be in the list'
        miles.size() == 4
        miles.tag.contains('Slides Available')
        miles.tag.contains('Monolithic Examples Done')
        miles.tag.contains('Microservice Examples Done')
        miles.tag.contains('Simple Examples Done')
    }
}

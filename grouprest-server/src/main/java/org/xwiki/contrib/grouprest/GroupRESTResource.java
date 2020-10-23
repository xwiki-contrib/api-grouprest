/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.xwiki.contrib.grouprest;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.contrib.grourest.model.jaxb.MemberGroups;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.XWikiResource;
import org.xwiki.user.group.GroupException;
import org.xwiki.user.group.GroupManager;
import org.xwiki.user.group.WikiTarget;

/**
 * @version $Id: 27d301df657f1738245f68a136a32e80d3aaa0f6 $
 */
@Component
@Named("org.xwiki.contrib.grouprest.GroupRESTResource")
@Path("/grouprest/groups/{member}")
@Singleton
public class GroupRESTResource extends XWikiResource
{
    @Inject
    private GroupManager groupManager;

    @Inject
    @Named("user/current")
    private DocumentReferenceResolver<String> userResolver;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private ConfigurationSource configuration;

    @Inject
    private QueryManager queryManager;

    /**
     * @param member the identifier used to find the member
     * @return the found groups
     * @throws GroupException when failing to search member groups
     * @throws QueryException when failing to resolve member id
     */
    @GET
    public MemberGroups get(@PathParam("member") String member) throws GroupException, QueryException
    {
        MemberGroups groups = new MemberGroups();

        groups.setInput(member);

        // Find the actual user reference
        DocumentReference reference = resolveReference(member);

        groups.setReference(this.serializer.serialize(reference));

        // Find the groups
        this.groupManager.getGroups(reference, WikiTarget.ALL, true)
            .forEach(group -> groups.getGoups().add(this.serializer.serialize(group)));

        return groups;
    }

    private DocumentReference resolveReference(String member) throws QueryException
    {
        String propertyName = this.configuration.getProperty("grouprest.user.mapping.property");
        if (propertyName != null) {
            String classReference = this.configuration.getProperty("grouprest.user.mapping.class", "XWiki.XWikiUsers");

            Query query = this.queryManager.createQuery("select doc.fullName from Document doc, doc.object("
                + classReference + ") obj WHERE obj." + propertyName + " = :value", Query.XWQL);

            query.setLimit(1);

            query.bindValue("value", member);

            List<String> documentNames = query.execute();
            if (!documentNames.isEmpty()) {
                return this.userResolver.resolve(documentNames.get(0));
            }
        }

        // Fallback on the standard user page name
        return this.userResolver.resolve(member);
    }
}

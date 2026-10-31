import copy
import json
import unittest
from test_install import m, Fake, project, version
import audit_jars


class ReleasePolicyTests(unittest.TestCase):
    def setUp(self):
        self.bindings=json.loads((m.ROOT/'modpack/source-bindings.json').read_text())
        self.fake=Fake();self.fake.projects={};self.fake.versions={}
        self.resolver=m.Resolver(self.fake,self.bindings)
    def mod(self,ident,slug,title,channel,source=None,deps=None,loaders=None):
        p=project(ident,title);p.update(slug=slug,source_url=source)
        self.fake.projects[ident]=p;self.fake.projects[slug]=p
        v=version(ident+'1',ident,deps,loaders);v['version_type']=channel
        self.fake.versions[v['id']]=v
        return p,v
    def test_cc_official_alpha_is_allowed_without_global_alpha(self):
        self.mod('gu7yAYhd','cc-tweaked','CC: Tweaked','alpha')
        key=self.resolver.target({'name':'CC: Tweaked'},self.bindings['targets']['cc-tweaked'])
        self.assertEqual('alpha',self.resolver.selected[key]['release_type'])
    def test_other_project_alpha_still_refused(self):
        self.mod('other','other','Other','alpha')
        with self.assertRaisesRegex(m.Blocked,'channels are excluded'):self.resolver.mr('other')
    def test_jei_beta_and_required_mezzconfig_beta(self):
        self.mod('7tEfOcA7','mezzconfig','MezzConfig','beta')
        self.mod('u6dRKJwZ','jei','Just Enough Items','beta',deps=[dict(dependency_type='required',project_id='7tEfOcA7',version_id=None)])
        key=self.resolver.target({'name':'JEI'},self.bindings['targets']['jei'])
        self.assertEqual(2,len(self.resolver.selected));self.assertEqual([],self.resolver.conflicts())
        self.assertEqual(['modrinth:7tEfOcA7'],self.resolver.selected[key]['dependencies'])
    def test_better_combat_required_playeranimator_alpha(self):
        self.mod('anim','playeranimator','Player Animator','alpha','https://github.com/KosmX/minecraftPlayerAnimator')
        self.mod('combat','better-combat','Better Combat','release','https://github.com/ZsoltMolnarrr/BetterCombat',deps=[dict(dependency_type='required',project_id='anim',version_id=None)])
        self.resolver.target({'name':'Better Combat'},self.bindings['targets']['better-combat'])
        self.assertEqual(2,len(self.resolver.selected));self.assertEqual('alpha',self.resolver.selected['modrinth:anim']['release_type'])
    def test_slug_policy_requires_reviewed_author_source(self):
        self.mod('impostor','playeranimator','Player Animator','alpha','https://github.com/unreviewed/fork')
        with self.assertRaisesRegex(m.Blocked,'identity mismatch'):self.resolver.mr('impostor')
    def test_official_issue_link_identifies_project_when_source_link_missing(self):
        project,_=self.mod('combat','better-combat','Better Combat','beta')
        project['issues_url']='https://github.com/ZsoltMolnarrr/BetterCombat/issues'
        self.resolver.mr('combat')
        self.assertEqual('beta',self.resolver.selected['modrinth:combat']['release_type'])
    def test_foreign_issue_repository_is_not_accepted(self):
        project,_=self.mod('combat','better-combat','Better Combat','beta')
        project['issues_url']='https://github.com/other/BetterCombat/issues'
        with self.assertRaisesRegex(m.Blocked,'identity mismatch'):self.resolver.mr('combat')
    def test_playeranimator_canonical_repository_resolves_under_better_combat(self):
        self.mod('anim','playeranimator','Player Animator','alpha','https://github.com/KosmX/fabricPlayerAnimation')
        self.mod('combat','better-combat','Better Combat','release','https://github.com/ZsoltMolnarrr/BetterCombat',deps=[dict(dependency_type='required',project_id='anim',version_id=None)])
        key=self.resolver.target({'name':'Better Combat'},self.bindings['targets']['better-combat'])
        self.assertEqual(['modrinth:anim'],self.resolver.selected[key]['dependencies'])
        self.assertEqual('anim',self.resolver.selected['modrinth:anim']['project_id'])
        self.assertEqual('alpha',self.resolver.selected['modrinth:anim']['release_type'])
        self.assertEqual('https://cdn.modrinth.com/data/anim/anim1.jar',self.resolver.selected['modrinth:anim']['file']['url'])
    def test_canonical_playeranimator_repository_does_not_identify_better_combat(self):
        self.mod('combat','better-combat','Better Combat','beta','https://github.com/KosmX/fabricPlayerAnimation')
        with self.assertRaisesRegex(m.Blocked,'identity mismatch'):self.resolver.mr('combat')
    def test_base_plan_keeps_resolved_artifacts_byte_for_byte(self):
        registry=json.loads((m.ROOT/'modpack/mod-registry.json').read_text())
        fake=Fake();fake.projects={};fake.versions={};bindings={'targets':{}}
        for t in registry['mods']:
            ident=t['id'];fake.projects[ident]=project(ident,t['name']);fake.versions[ident]=version(ident,ident)
            bindings['targets'][ident]=dict(modrinth_slug_hint=ident,accepted_titles=[t['name']])
        old=m.resolve(registry,bindings,fake)
        for v in list(fake.versions.values()):
            newer=copy.deepcopy(v);newer['id']+='new';newer['date_published']='2027-01-01';fake.versions[newer['id']]=newer
        repaired=m.resolve(registry,bindings,fake,base_plan=old)
        self.assertEqual(old['mods'],repaired['mods'])
    def test_permitted_alpha_does_not_permit_fabric(self):
        self.mod('gu7yAYhd','cc-tweaked','CC: Tweaked','alpha',loaders=['fabric'])
        with self.assertRaisesRegex(m.Blocked,'no compatible'):self.resolver.mr('gu7yAYhd')
    def test_stable_preferred_over_newer_beta(self):
        old=version(date='2026-01-01');new=version('v2',date='2026-02-01');new['version_type']='beta'
        self.assertEqual('v1',m.choose([old,new],release_types=['release','beta'])['id'])
    def test_error_identifies_actual_dependency_and_channel(self):
        self.mod('root','root','Root','release',deps=[dict(dependency_type='required',project_id='library',version_id=None)])
        self.mod('library','library','Example Library','alpha')
        with self.assertRaisesRegex(m.Blocked,r'Example Library.*alpha'):self.resolver.mr('root')
    def test_magic_unidentified_targets_are_not_queried_or_substituted(self):
        class Denied:
            def json(self,url):raise m.Blocked('fixture unavailable')
        registry=json.loads((m.ROOT/'modpack/mod-registry.json').read_text())
        plan=m.resolve(registry,self.bindings,Denied())
        self.assertEqual(40,len(plan['targets']))
        for ident in ['thaumcraft-4-reforged','botania-revived']:
            target=next(t for t in plan['targets'] if t['id']==ident)
            self.assertFalse(target['selected']);self.assertEqual('deferred-identity',target['status'])
        skills=next(t for t in plan['targets'] if t['id']=='pufferfishs-skills')
        self.assertEqual('optional-unresolved',skills['status']);self.assertFalse(skills['selected'])
        self.assertIn('Core skills',skills['reason'])
    def test_explicit_deferrals_allow_install_validation_and_keep_all_40(self):
        registry=json.loads((m.ROOT/'modpack/mod-registry.json').read_text())
        fake=Fake();fake.projects={};fake.versions={};bindings={'targets':{}}
        for target in registry['mods']:
            ident=target['id'];fake.projects[ident]=project(ident,target['name']);fake.versions[ident]=version(ident,ident)
            bindings['targets'][ident]=dict(modrinth_slug_hint=ident,accepted_titles=[target['name']])
        for ident in ['thaumcraft-4-reforged','botania-revived','pufferfishs-skills']:
            bindings['targets'][ident]=copy.deepcopy(self.bindings['targets'][ident])
        plan=m.resolve(registry,bindings,fake)
        records,missing=m.validate_plan(plan)
        self.assertEqual([],missing);self.assertEqual(40,len(plan['targets']))
        self.assertEqual(36,len(records));self.assertFalse(plan['runtime_verified'])

    def test_skills_stale_slug_is_removed_without_alias_substitution(self):
        binding=self.bindings['targets']['pufferfishs-skills']
        self.assertIsNone(binding['modrinth_slug_hint'])
        self.assertEqual({'pufferfishsskills'},{m.normal(t) for t in binding['accepted_titles']})
        class Found(Fake):
            def json(inner,url):
                if '/search?' in url:return {'hits':[{'project_id':'root','title':"Pufferfish's Skills"}]}
                return super().json(url)
        fake=Found();fake.projects['root']['title']="Pufferfish's Skills"
        key=m.Resolver(fake,self.bindings).target({'name':"Pufferfish's Skills"},binding)
        self.assertEqual('modrinth:root',key)


class PlatformRangeTests(unittest.TestCase):
    def test_numeric_ranges_use_numeric_not_lexical_order(self):
        self.assertTrue(audit_jars.numeric_range_contains('21.1.264','[21.1.238,)'))
        self.assertFalse(audit_jars.numeric_range_contains('21.1.9','[21.1.238,)'))
    def test_exclusive_upper_boundary_rejects_wrong_game_version(self):
        self.assertFalse(audit_jars.numeric_range_contains('1.21.1','[1.21, 1.21.1)'))
        self.assertTrue(audit_jars.numeric_range_contains('1.21.1','[1.21.1, 1.21.2)'))
    def test_exact_range_and_unknown_qualifiers(self):
        self.assertTrue(audit_jars.numeric_range_contains('1.21.1','[1.21.1]'))
        self.assertIsNone(audit_jars.numeric_range_contains('1.21.1','[1.21-experimental,)'))

if __name__=='__main__':unittest.main()
